package de.typology.predictors;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.FieldValueFilter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import de.typology.utils.Algo;
import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public class LuceneNGramSearcher {

	private ArrayList<IndexSearcher> index;

	public LuceneNGramSearcher() {
		this.index = new ArrayList<IndexSearcher>();
		try {
			for (int i = 2; i < 6; i++) {
				Directory directory;
				DirectoryReader directoryReader;
				directory = FSDirectory.open(new File(
						Config.get().nGramIndexPath + i + "/"));
				directoryReader = DirectoryReader.open(directory);
				this.index.add(new IndexSearcher(directoryReader));

			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

	}

	public void query(String q, String prefix, String match) {
		IOHelper.log(q + " \tPREFIX: " + prefix + " \tMATCH: " + match);
		HashMap<String, Float> result = this.search(q, prefix, 12);
		Algo<String, Float> a = new Algo<String, Float>();
		TreeMap<Float, Set<String>> topkSuggestions = a.getTopkElements(result,
				5);
		int topkCnt = 0;

		for (Float score : topkSuggestions.descendingKeySet()) {
			for (String suggestion : topkSuggestions.get(score)) {
				topkCnt++;
				if (suggestion.equals(match)) {
					IOHelper.log("HIT\tRANK: " + topkCnt + " \tPREFIXLENGHT: "
							+ prefix.length());
					if (topkCnt == 1) {
						IOHelper.log("KSS: "
								+ (match.length() - prefix.length())
								+ " \tPREFIXLENGHT: " + prefix.length());
					}
					return;
				}
			}
		}
		IOHelper.log("NOTHING\tPREFIXLENGTH: " + prefix.length());
	}

	// docs at:
	// http://www.ibm.com/developerworks/library/os-apache-lucenesearch/
	// http://stackoverflow.com/questions/468405/how-to-incorporate-multiple-fields-in-queryparser
	// http://oak.cs.ucla.edu/cs144/projects/lucene/index.html in chapter 2
	public HashMap<String, Float> search(String q, String prefix,
			int numIntermediateLists) {
		HashMap<String, Float> result = new HashMap<String, Float>();
		try {
			SortField sortField = new SortField("cnt", SortField.Type.FLOAT,
					true);
			Sort sort = new Sort(sortField);

			Analyzer analyzer = new KeywordAnalyzer();
			QueryParser queryParser = new QueryParser(Version.LUCENE_40, "src",
					analyzer);

			queryParser.setLowercaseExpandedTerms(false);

			String[] terms = this.prepareQuery(q);
			if (terms == null) {
				return null;
			}

			ArrayList<TopDocs> hits = new ArrayList<TopDocs>();

			for (int i = 0; i < terms.length; i++) {
				String special = "src:\"" + terms[i] + "\"";
				if (prefix.length() > 0) {
					special = "src:\"" + terms[i] + "\" AND tgt:" + prefix
							+ "*";
				}

				TopDocs results = this.index.get(i)
						.search(queryParser.parse(special),
								new FieldValueFilter("cnt"),
								numIntermediateLists, sort);

				hits.add(results);

				for (ScoreDoc scoreDoc : results.scoreDocs) {
					Document doc = this.index.get(i).doc(scoreDoc.doc);

					String key = doc.get("tgt");
					Float value = Float.parseFloat(doc.get("cnt"));

					if (result.containsKey(key)) {
						result.put(key, value + result.get(key));
					} else {
						result.put(key, value);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}

	// query format at least
	// input: w0 w1 w2 w3
	// output: {"w3", "w2 w3", "w1 w2 w3", "w0 w1 w2 w3"}
	private String[] prepareQuery(String q) {
		String[] words = q.split(" ");
		if (words.length == 1) {
			String[] result = new String[4];
			result[0] = words[0];
			return result;
		} else if (words.length == 2) {
			String[] result = new String[4];
			result[0] = words[1];
			result[1] = words[0] + " " + words[1];
			return result;

		} else if (words.length == 3) {
			String[] result = new String[3];
			result[0] = words[2];
			result[1] = words[1] + " " + words[2];
			result[2] = words[0] + " " + words[1] + " " + words[2];
			return result;

		} else if (words.length == 4) {
			String[] result = new String[4];
			result[0] = words[3];
			result[1] = words[2] + " " + words[3];
			result[2] = words[1] + " " + words[2] + " " + words[3];
			result[3] = words[0] + " " + words[1] + " " + words[2] + " "
					+ words[3];
			return result;
		}
		return null;
	}
}
