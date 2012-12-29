package de.typology.predictors;

import java.io.BufferedWriter;
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
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.Version;

import de.typology.interfaces.Searchable;
import de.typology.utils.Algo;
import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public class LuceneNGramSearcher implements Searchable {

	private ArrayList<IndexSearcher> index;
	private Sort sort;
	private QueryParser queryParser;
	private FieldValueFilter fieldValueFilter;
	private float[][] weights;

	// done:<
	public LuceneNGramSearcher() {
		this.index = new ArrayList<IndexSearcher>();
		try {
			for (int i = 2; i < 6; i++) {
				// i=ngram index type
				Directory directory;
				DirectoryReader directoryReader;
				if (Config.get().loadIndexToRAM) {
					directory = MMapDirectory.open(new File(
							Config.get().nGramIndexPath + i + "/"));
				} else {
					directory = FSDirectory.open(new File(
							Config.get().nGramIndexPath + i + "/"));
				}
				directoryReader = DirectoryReader.open(directory);
				this.index.add(new IndexSearcher(directoryReader));

			}
			// >
			// copy&paste:<
			SortField sortField = new SortField("cnt", SortField.Type.FLOAT,
					true);
			this.sort = new Sort(sortField);
			Analyzer analyzer = new KeywordAnalyzer();
			this.queryParser = new QueryParser(Version.LUCENE_40, "src",
					analyzer);
			this.queryParser.setLowercaseExpandedTerms(false);
			this.fieldValueFilter = new FieldValueFilter("cnt");
			this.weights = new float[100][Config.get().nGramLength];
			for (int i = 0; i < 100; i++) {
				for (int j = 0; j < Config.get().nGramLength; j++) {
					this.weights[i][j] = 0;
				}
			}
			// >
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

	}

	// done (except comments)<
	@Override
	public int query(String q, String prefix, String match,
			int intermediateListLength, int k) {

		// IOHelper.logLearn("TYPOLOGY - QUERY: " + q + " PREFIXLENGTH: "
		// + prefix.length() + " MATCH: " + match);

		HashMap<String, Float> result = this.search(q, prefix,
				intermediateListLength, match);
		Algo<String, Float> a = new Algo<String, Float>();
		TreeMap<Float, Set<String>> topkSuggestions = a.getTopkElements(result,
				k);
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
						// + " \tPREFIXLENGHT: " + prefix.length() was removed
						// in TypologySearcher
						);
					}
					return topkCnt;
				}
			}
		}
		IOHelper.log("NOTHING\tPREFIXLENGTH: " + prefix.length());
		return -1;
	}// >

	// not sure:<

	// docs at:
	// http://www.ibm.com/developerworks/library/os-apache-lucenesearch/
	// http://stackoverflow.com/questions/468405/how-to-incorporate-multiple-fields-in-queryparser
	// http://oak.cs.ucla.edu/cs144/projects/lucene/index.html in chapter 2
	@Override
	public HashMap<String, Float> search(String q, String prefix,
			int numIntermediateLists, String match) {
		HashMap<String, Float> result = new HashMap<String, Float>();
		try {

			String[] terms = this.prepareQuery(q);
			// extend prepareQuery?
			if (terms == null) {
				return null;
			}
			int ngram = 0;

			// ArrayList<TopDocs> hits = new ArrayList<TopDocs>();

			for (int i = 0; i < terms.length; i++) {
				String special = "src:" + terms[i];
				if (prefix.length() > 0) {
					special = special.concat(" AND tgt:" + prefix + "*");
				}

				TopDocs results = this.index.get(i).search(
						this.queryParser.parse(special), this.fieldValueFilter,
						numIntermediateLists, this.sort);

				// hits.add(results);
				int rank = 1;
				for (ScoreDoc scoreDoc : results.scoreDocs) {
					Document doc = this.index.get(i).doc(scoreDoc.doc);

					String key = doc.get("tgt");
					Float value = Float.parseFloat(doc.get("cnt"));

					if (key.equals(match)) {
						this.weights[prefix.length()][ngram + 1] += 1 / (float) rank;
					}
					rank++;
					// String res = "";
					// if (key.equals(match)) {
					// res = " \tHIT";
					// } else {
					// res = " \tNOMATCH";
					// }
					// IOHelper.logLearn("FROM: " + terms[i] + " \tEDGETYPE: "
					// + (edge + 1) + " \t RANK: " + rank++
					// + " \tPREDICTS: " + key + "\t SCORE: " + value
					// + res);

					if (result.containsKey(key)) {
						result.put(key, value + result.get(key));
					} else {
						result.put(key, value);
					}
				}
				ngram++;
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}

	// >

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

	// copy&paste:<
	@Override
	public void saveWeights(int numQueries) {
		BufferedWriter bw = IOHelper.openWriteFile("weights");
		try {
			for (int i = 0; i < 10; i++) {
				bw.write("PREFIXLENGTH: " + i);
				float max = 0;
				for (int j = 1; j < Config.get().nGramLength; j++) {
					if (this.weights[i][j] > max) {
						max = this.weights[i][j];
					}
				}
				// TODO: can be divided by numQueries
				for (int j = 1; j < Config.get().nGramLength; j++) {
					bw.write(" w" + j + ": " + (int) this.weights[i][j] * 1000
							/ max);
				}
				bw.write("\n");
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}// >
}
