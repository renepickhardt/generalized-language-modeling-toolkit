package de.typology.predictors;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
import de.typology.utils.Pair;

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

	public void query(String q, String prefix, String match) {
		IOHelper.log(q + " \tPREFIX: " + prefix + " \tMATCH: " + match);
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
			ArrayList<TopDocs> hits = new ArrayList<TopDocs>();

			for (int i = terms.length - 1; i >= Math.max(0, terms.length - 4); i--) {
				String special = "src:" + terms[i];
				if (prefix.length() > 0) {
					special = "src:" + terms[i] + " AND tgt:" + prefix + "*";
				}

				TopDocs results = this.index.get(edge)
						.search(queryParser.parse(special),
								new FieldValueFilter("cnt"),
								numIntermediateLists, sort);
				hits.add(results);
				edge++;
			}

			// NRA Algorithm
			this.nra(numIntermediateLists, hits);
			// for (ScoreDoc scoreDoc : results.scoreDocs) {
			// Document doc = this.index.get(edge).doc(scoreDoc.doc);
			//
			// String key = doc.get("tgt");
			// Float value = Float.parseFloat(doc.get("cnt"));
			//
			// if (result.containsKey(key)) {
			// result.put(key, value + result.get(key));
			// } else {
			// result.put(key, value);
			// }
			// }

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

	private HashMap<String, Pair<Float, Float>> keyIndex;
	private TreeMap<Float, Set<String>> lowerIndex;
	private TreeMap<Float, Set<String>> upperIndex;
	private HashMap<String, ArrayList<Pair<Boolean, Float>>> buffer;

	private void nra(int numIntermediateLists, ArrayList<TopDocs> hits)
			throws IOException {
		this.keyIndex = new HashMap<String, Pair<Float, Float>>();
		this.lowerIndex = new TreeMap<Float, Set<String>>();
		this.upperIndex = new TreeMap<Float, Set<String>>();
		this.buffer = new HashMap<String, ArrayList<Pair<Boolean, Float>>>();
		for (int j = 0; j < numIntermediateLists; j++) {
			String[] keys = new String[hits.size()];
			float[] lowerBounds = new float[hits.size()];
			for (int i = 0; i < hits.size(); i++) {
				// TRY ARRAY OUT OF BOUND CATCHING
				try {
					TopDocs td = hits.get(i);
					ScoreDoc[] sd = td.scoreDocs;
					ScoreDoc d = sd[j];
					Document doc = this.index.get(i).doc(d.doc);
					lowerBounds[i] = Float.parseFloat(doc.get("cnt"));
					keys[i] = doc.get("tgt");
				} catch (ArrayIndexOutOfBoundsException e) {
					e.printStackTrace();
					return;
				}
			}
			this.updateBuffer(keys, lowerBounds);
			int i = 0;
			final int k = 5;
			float lowerBound = 0;
			for (Float key : this.lowerIndex.descendingKeySet()) {
				for (String word : this.lowerIndex.get(key)) {
					i++;
					if (i >= k) {
						break;
					}
				}
				if (i >= k) {
					lowerBound = key;
					break;
				}
			}

			i = 0;
			boolean flag = true;
			for (Float key : this.upperIndex.descendingKeySet()) {
				if (flag) {
					for (String word : this.upperIndex.get(key)) {
						i++;
						if (i >= k) {
							break;
						}
					}
					if (i >= k) {
						flag = false;
					}
				} else {
					if (key.compareTo(new Float(lowerBound)) < 0) {
						this.outPut(k);
						return; // FOUND TOPK elements
					}
				}
			}
		}

	}

	private void outPut(int k) {
		int i = 0;
		for (Float key : this.lowerIndex.descendingKeySet()) {
			for (String word : this.lowerIndex.get(key)) {
				i++;
				System.out.println(word + " \t" + key);
				if (i >= k) {
					break;
				}
			}
			if (i >= k) {
				break;
			}
		}
	}

	/**
	 * for every word we have a list of floats . the nth list stands for the
	 * score for the word from the nth list
	 * 
	 * the list is actually a list of pairs where the first value is false if
	 * the score for the word from this list has not been seen
	 * 
	 * 
	 * @param buffer
	 * @param keys
	 * @param lowerBounds
	 * @param bound
	 */
	private void updateBuffer(String[] keys, float[] lowerBounds) {
		int i = 0;
		int max = keys.length;
		while (i < max) {
			if (this.buffer.containsKey(keys[i])) {
				ArrayList<Pair<Boolean, Float>> list = this.buffer.get(keys[i]);
				Pair<Boolean, Float> p = list.get(i);
				p.setFirst(true);
				p.setSecond(lowerBounds[i]);
				list.set(i, p);
				this.buffer.put(keys[i], list);
			} else {
				ArrayList<Pair<Boolean, Float>> list = new ArrayList<Pair<Boolean, Float>>();
				for (int j = 0; j < max; j++) {
					if (i == j) {
						list.add(new Pair<Boolean, Float>(true, lowerBounds[i]));
					} else {
						list.add(new Pair<Boolean, Float>(false, new Float(0)));
					}
				}
			}
			i++;
		}

		// inefficient to do this for all lists every step?
		for (String key : this.buffer.keySet()) {
			ArrayList<Pair<Boolean, Float>> list = this.buffer.get(key);
			float upper = 0;
			float lower = 0;
			i = 0;
			for (Pair<Boolean, Float> p : list) {
				if (p.getFirst()) {
					upper += p.getSecond();
					lower += p.getSecond();
				} else {
					upper += lowerBounds[i];
				}
				i++;
			}
			// maintain the top elements together with bounds?
			this.maintain(key, lower, upper);
		}
	}

	/**
	 * what is a good data structure for such a triplet? requirements: lookup of
	 * key must be O(1) data structure must be sorted by lower.
	 * 
	 * 3 indices that are redundantly stored... is so far the best guess
	 * 
	 * @param key
	 * @param lower
	 * @param upper
	 */
	private void maintain(String key, float lower, float upper) {
		if (!this.keyIndex.containsKey(key)) {
			this.keyIndex.put(key, new Pair<Float, Float>(lower, upper));
			Set<String> s = new HashSet<String>();
			s.add(key);
			this.lowerIndex.put(lower, s);
			this.upperIndex.put(upper, s);
		} else {
			Pair<Float, Float> oldLowerUpper = this.keyIndex.get(key);
			this.keyIndex.put(key, new Pair<Float, Float>(lower, upper));

			if (oldLowerUpper.getFirst().equals(lower)) {
			} else {
				Set<String> s = this.lowerIndex.get(oldLowerUpper.getFirst());
				s.remove(key);
				this.lowerIndex.put(oldLowerUpper.getFirst(), s);
				s = this.lowerIndex.get(lower);
				s.add(key);
				this.lowerIndex.put(lower, s);
			}

			if (oldLowerUpper.getSecond().equals(upper)) {
			} else {
				Set<String> s = this.upperIndex.get(oldLowerUpper.getSecond());
				s.remove(key);
				this.upperIndex.put(oldLowerUpper.getSecond(), s);
				s = this.upperIndex.get(upper);
				s.add(key);
				this.upperIndex.put(upper, s);
			}
		}
	}
}
