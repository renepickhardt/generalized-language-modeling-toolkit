package de.typology.predictors;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.PriorityQueue;

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

				class MyCollector extends Collector {
					class CustomQueue extends PriorityQueue<Document> {

						public CustomQueue(int maxSize) {
							super(maxSize);
							// TODO Auto-generated constructor stub
						}

						@Override
						protected boolean lessThan(Document arg0, Document arg1) {
							String tmp = arg0.get("cnt");
							if (tmp == null) {
								return false;
							}
							Float f0 = Float.parseFloat(tmp);
							tmp = arg1.get("cnt");
							if (tmp == null) {
								return true;
							}
							Float f1 = Float.parseFloat(tmp);
							return f0.compareTo(f1) < 0;
						}
					}

					CustomQueue _queue = null;
					IndexReader _currentReader;

					public MyCollector(int maxSize) {
						this._queue = new CustomQueue(maxSize);
					}

					@Override
					public boolean acceptsDocsOutOfOrder() {
						return true;
					}

					@Override
					public void collect(int arg0) throws IOException {
						this._queue.insertWithOverflow(this._currentReader
								.document(arg0));
					}

					@Override
					public void setNextReader(AtomicReaderContext arg0)
							throws IOException {
						this._currentReader = arg0.reader();

					}

					@Override
					public void setScorer(Scorer arg0) throws IOException {
						// TODO Auto-generated method stub

					}

				}

				MyCollector mc = new MyCollector(this.joinLength);
				this.index.get(i).search(this.queryParser.parse(special),
						this.fieldValueFilter, mc);
				Document doc = null;
				int rank = 1;
				while ((doc = mc._queue.pop()) != null) {
					String key = "";
					Float value = new Float(0);
					try {
						key = doc.get("tgt");
						value = Float.parseFloat(doc.get("cnt"));
					} catch (Exception e) {
						continue;
					}
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
				// TopDocs results = this.index.get(i).search(
				// this.queryParser.parse(special), this.fieldValueFilter,
				// this.joinLength, this.sort);

				// hits.add(results);

				// << comment out for testing priority queue
				// int rank = 1;
				// for (ScoreDoc scoreDoc : results.scoreDocs) {
				// Document doc = this.index.get(i).doc(scoreDoc.doc);
				// String key = "";
				// Float value = new Float(0);
				// try {
				// key = doc.get("tgt");
				// value = Float.parseFloat(doc.get("cnt"));
				// } catch (Exception e) {
				// continue;
				// }
				// if (key.equals(match)) {
				// this.learningWeights[prefix.length()][i + 1] += 1 / (float)
				// rank;
				// }
				// rank++;
				// comment out >>
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
				// << comment out for testing priority queue

				// if (result.containsKey(key)) {
				// result.put(key, value + result.get(key));
				// } else {
				// result.put(key, value);
				// }
				// }
				// << comment out for testing priority queue

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

	public String[] getQueryNGrams(String q) {
		String[] words = q.split(" ");
		int l = words.length - 1;
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
