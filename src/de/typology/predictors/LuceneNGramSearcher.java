package de.typology.predictors;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.MMapDirectory;

import de.typology.utils.Config;

public class LuceneNGramSearcher extends Searcher {

	private ArrayList<IndexSearcher> index;

	// done:<
	public LuceneNGramSearcher(int n, int k, int joinLength) {
		super(n, k, joinLength);
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
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// not sure:<

	// docs at:
	// http://www.ibm.com/developerworks/library/os-apache-lucenesearch/
	// http://stackoverflow.com/questions/468405/how-to-incorporate-multiple-fields-in-queryparser
	// http://oak.cs.ucla.edu/cs144/projects/lucene/index.html in chapter 2
	@Override
	public HashMap<String, Float> search(String q, String prefix, String match) {
		HashMap<String, Float> result = new HashMap<String, Float>();
		try {

			String[] terms = this.getQueryNGrams(q);
			// extend prepareQuery?
			if (terms == null) {
				return null;
			}

			// ArrayList<TopDocs> hits = new ArrayList<TopDocs>();

			for (int i = 0; i < terms.length; i++) {
				String special = "src:" + terms[i];
				if (prefix.length() > 0) {
					special = special.concat(" AND tgt:" + prefix + "*");
				}

				TopDocs results = this.index.get(i).search(
						this.queryParser.parse(special), this.fieldValueFilter,
						this.joinLength, this.sort);

				// hits.add(results);
				int rank = 1;
				for (ScoreDoc scoreDoc : results.scoreDocs) {
					Document doc = this.index.get(i).doc(scoreDoc.doc);

					String key = doc.get("tgt");
					Float value = Float.parseFloat(doc.get("cnt"));

					if (key.equals(match)) {
						this.learningWeights[prefix.length()][i + 1] += 1 / (float) rank;
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

	// TODO: update this! to be correct: see EvalHelper probably good idea a
	// prepareQuery function in the interface
	// query format at least
	// input: w0 w1 w2 w3
	// output: {"w3", "w2 w3", "w1 w2 w3", "w0 w1 w2 w3"}
	// private String[] prepareQuery(String q) {
	// }

	@Override
	public String getFileName() {
		String name = "";
		if (Config.get().weightedPredictions) {
			name = name.concat("weighted-");
		}
		name = name.concat("nGram-" + this.N + "-joinLengh-" + this.joinLength
				+ "-" + Config.get().sampleRate + Config.get().splitDataRatio
				+ ".log");
		// TODO Auto-generated method stub
		return name;
	}

	@Override
	public String prepareQuery(String[] words, int n) {
		String query = "";
		for (int i = 0; i < words.length; i++) {
			words[i] = QueryParser.escape(words[i]);
		}
		if (n == 5) {
			query = words[0] + " " + words[1] + " " + words[2] + " " + words[3];
		} else if (n == 4) {
			query = words[1] + " " + words[2] + " " + words[3];
		} else if (n == 3) {
			query = words[2] + " " + words[3];
		} else if (n == 2) {
			query = words[4];
		}
		return query;
	}

	public String[] getQueryNGrams(String q) {
		String[] words = q.split(" ");
		int l = words.length;
		if (words.length == 1) {
			String[] result = new String[4];
			result[0] = words[l];
			return result;
		} else if (words.length == 2) {
			String[] result = new String[4];
			result[0] = words[l];
			result[1] = words[l - 1] + " " + words[l];
			return result;

		} else if (words.length == 3) {
			String[] result = new String[3];
			result[0] = words[l];
			result[1] = words[l - 1] + " " + words[l];
			result[2] = words[l - 2] + " " + words[l - 1] + " " + words[l];
			return result;

		} else if (words.length == 4) {
			String[] result = new String[4];
			result[0] = words[l];
			result[1] = words[l - 1] + " " + words[l];
			result[2] = words[l - 2] + " " + words[l - 1] + " " + words[l];
			result[3] = words[l - 3] + " " + words[l - 2] + " " + words[l - 1]
					+ " " + words[l];
			return result;
		}
		return null;
	}
}
