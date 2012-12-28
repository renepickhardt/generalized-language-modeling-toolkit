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
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.Version;

import de.typology.utils.Algo;
import de.typology.utils.Config;
import de.typology.utils.IOHelper;

/**
 * typology retrieval implmented on lucene
 * 
 * @author rpickhardt, Martin Koerner
 * 
 */
public class LuceneTypologySearcher {

	private ArrayList<IndexSearcher> index;

	public LuceneTypologySearcher() {
		this.index = new ArrayList<IndexSearcher>();
		try {
			for (int i = 1; i < 5; i++) {
				Directory directory;
				DirectoryReader directoryReader;
				// http://lucene.apache.org/core/4_0_0/core/org/apache/lucene/store/MMapDirectory.html
				directory = MMapDirectory.open(new File(Config.get().indexPath
						+ i + "/"));

				// directory = FSDirectory.open(new File(Config.get().indexPath
				// + i + "/"));

				// http://www.avajava.com/tutorials/lessons/how-do-i-convert-a-file-system-index-to-a-memory-index.html

				// http://stackoverflow.com/questions/673887/using-ramdirectory
				// 2 GB limit on index size

				// Directory memoryDirectory = new RAMDirectory(directory);

				// above is not a good way for big indices according to
				// http://lucene.apache.org/core/4_0_0/core/org/apache/lucene/store/RAMDirectory.html

				directoryReader = DirectoryReader.open(directory);
				this.index.add(new IndexSearcher(directoryReader));

			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param args
	 * @throws ParseException
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException, ParseException {
		LuceneTypologySearcher lts = new LuceneTypologySearcher();

		lts.query("Ich gehe über die", "");
		lts.query("Bla Ich gehe über die", "");
		lts.query("eines schoenen", "");
		lts.query("das wandern ist des", "");
		lts.query("Koblenz ist eine Stadt am", "");
		lts.query("Der Frankfurter Flughafen", "");

		HashMap<String, Float> result = lts.search("Bla Ich gehe über die", "",
				12);

		for (int i = 0; i < 10; i++) {
			result = lts.search("Bla Ich gehe über die", "", 12 - i);

			Algo<String, Float> a = new Algo<String, Float>();
			TreeMap<Float, Set<String>> topkSuggestions = a.getTopkElements(
					result, 5);
			int topkCnt = 0;

			for (Float score : topkSuggestions.descendingKeySet()) {
				System.out.println(score);
				for (String suggestion : topkSuggestions.get(score)) {
					System.out.println("  " + suggestion);
				}
			}
		}
	}

	public void query(String q, String prefix) {
		IOHelper.log(q + " PREFIX: " + prefix);
		HashMap<String, Float> result = this.search(q, prefix, 12);
		Algo<String, Float> a = new Algo<String, Float>();
		TreeMap<Float, Set<String>> topkSuggestions = a.getTopkElements(result,
				5);
		;
		int topkCnt = 0;

		for (Float score : topkSuggestions.descendingKeySet()) {
			System.out.println(score);
			for (String suggestion : topkSuggestions.get(score)) {
				System.out.println("  " + suggestion);
			}
		}
	}

	public int query(String q, String prefix, String match) {
		HashMap<String, Float> result = this.search(q, prefix, 12);
		Algo<String, Float> a = new Algo<String, Float>();
		TreeMap<Float, Set<String>> topkSuggestions = a.getTopkElements(result,
				5);
		;
		int topkCnt = 0;

		for (Float score : topkSuggestions.descendingKeySet()) {
			for (String suggestion : topkSuggestions.get(score)) {
				topkCnt++;
				if (suggestion.equals(match)) {
					IOHelper.logResult("HIT\tRANK: " + topkCnt
							+ " \tPREFIXLENGHT: " + prefix.length());
					if (topkCnt == 1) {
						IOHelper.logResult("KSS: "
								+ (match.length() - prefix.length())
								+ " \tPREFIXLENGHT: " + prefix.length());
					}
					return topkCnt;
				}
			}
		}
		IOHelper.logResult("NOTHING\tPREFIXLENGTH: " + prefix.length());
		return -1;
	}

	// docs at:
	// http://www.ibm.com/developerworks/library/os-apache-lucenesearch/
	// http://stackoverflow.com/questions/468405/how-to-incorporate-multiple-fields-in-queryparser
	// http://oak.cs.ucla.edu/cs144/projects/lucene/index.html in chapter 2
	public HashMap<String, Float> search(String q, String prefix,
			int numIntermediateLists) {

		long startTime = System.currentTimeMillis();

		HashMap<String, Float> result = new HashMap<String, Float>();
		try {
			SortField sortField = new SortField("cnt", SortField.Type.FLOAT,
					true);
			Sort sort = new Sort(sortField);
			Analyzer analyzer = new KeywordAnalyzer();
			QueryParser queryParser = new QueryParser(Version.LUCENE_40, "src",
					analyzer);
			queryParser.setLowercaseExpandedTerms(false);

			String[] terms = q.split(" ");
			int edge = 0;

			for (int i = terms.length - 1; i >= Math.max(0, terms.length - 4); i--) {
				String special = "src:" + terms[i];
				if (prefix.length() > 0) {
					special = "src:" + terms[i] + " AND tgt:" + prefix + "*";
				}

				TopDocs results = this.index.get(edge)
						.search(queryParser.parse(special),
								new FieldValueFilter("cnt"),
								numIntermediateLists, sort);

				for (ScoreDoc scoreDoc : results.scoreDocs) {
					Document doc = this.index.get(edge).doc(scoreDoc.doc);

					String key = doc.get("tgt");
					Float value = Float.parseFloat(doc.get("cnt"));

					if (result.containsKey(key)) {
						result.put(key, value + result.get(key));
					} else {
						result.put(key, value);
					}
				}
				edge++;
			}

		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		long endTime = System.currentTimeMillis();
		// IOHelper.log(endTime - startTime + " milliseconds for searching " +
		// q);
		return result;
	}

}
