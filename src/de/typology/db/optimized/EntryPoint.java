package de.typology.db.optimized;

//TODO: two options. eather serialize rank value to string and append it to every string or extend SuggestTree to also store the values. second would be more efficient but also more anoying

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.kernel.EmbeddedGraphDatabase;

import de.typology.db.optimized.SuggestTree.SuggestionList;
import de.typology.utils.ConfigHelper;

public class EntryPoint {
	private static final int SUGGESTIONS_COUNT = 50;
	private static final int WORD_COUNT = 0;
	static public HashMap<String, SuggestTree<Double>> db1 = new HashMap<String, SuggestTree<Double>>();
	static public HashMap<String, SuggestTree<Double>> db2 = new HashMap<String, SuggestTree<Double>>();
	static public HashMap<String, SuggestTree<Double>> db3 = new HashMap<String, SuggestTree<Double>>();

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Map<String, Double> input = new HashMap<String, Double>();
		Comparator<Double> c = new Comparator<Double>() {

			@Override
			public int compare(Double e1, Double e2) {
				return -Double.compare(e1, e2);
			}
		};

		EmbeddedGraphDatabase graphDB = new EmbeddedGraphDatabase(
				"/home/rpickhardt/data/source code/workspace/db_ngrams_ucsc_r5.db");

		int cnt = 0;
		long createIndexStart = System.currentTimeMillis();
		for (Node n : graphDB.getAllNodes()) {
			if (n.hasProperty(ConfigHelper.COUNT_KEY)) {
				Integer count = (Integer) n.getProperty(ConfigHelper.COUNT_KEY);
				if (count > WORD_COUNT) {
					cnt++;
					String word = (String) n.getProperty(ConfigHelper.NAME_KEY);
					// if (cnt%10==0)System.out.println(word +
					// "\t\tbuild 1st tree:");
					CreateSuggestTree(word, n, 1);
					CreateSuggestTree(word, n, 2);
					CreateSuggestTree(word, n, 3);

					if (cnt < 10 || cnt % 100 == 0) {
						// for (String w: db1.keySet()){
						SuggestionList list = db1.get(word).getBestSuggestions(
								"");
						System.out.println(cnt + "\t" + word + " ..."
								+ db1.get(word).sizeInfo());
						OutputList(list);
						list = db2.get(word).getBestSuggestions("");
						System.out.println(cnt + "\t" + word + " *** + ..."
								+ db2.get(word).sizeInfo());
						OutputList(list);
						list = db3.get(word).getBestSuggestions("");
						System.out.println(cnt + "\t" + word + " *** *** + ..."
								+ db3.get(word).sizeInfo());
						OutputList(list);
						// }
					}
				}
			}
			if (cnt > 102) {
				break;
			}
		}
		System.out.println(cnt);
		long createIndexEnd = System.currentTimeMillis();
		System.out.println("time to create the index: "
				+ (createIndexEnd - createIndexStart));

		RunTimeTest();

		System.out.println("indexing summaray:");
		System.out.println("k = " + SUGGESTIONS_COUNT);
		System.out.println("threashold = " + WORD_COUNT);
		System.out.println("number of indexed words: " + cnt);
		System.out.println("index build time (in ms): "
				+ (createIndexEnd - createIndexStart));

		graphDB.shutdown();
	}

	private static void RunTimeTest() {
		long start = System.currentTimeMillis();
		int cnt = 0;
		long ett = 0;
		long dtt = 0;
		try {
			BufferedReader br = new BufferedReader(
					new FileReader(
							"/home/rpickhardt/data/source code/workspace/complete/10k_sentences.txt"));
			String line;
			SuggestionList list1 = null;
			SuggestionList list2 = null;
			SuggestionList list3 = null;

			while ((line = br.readLine()) != null) {
				String[] values = line.split(" ");
				if (values.length < 5) {
					continue;
				}
				String prefix = values[3].substring(0,
						Math.min(values[3].length() / 2, 5)); // ""+values[3].charAt(0)

				if (db1.containsKey(values[2])) {
					list1 = db1.get(values[2]).getBestSuggestions(prefix);
				} else {
					continue;
				}
				if (db2.containsKey(values[1])) {
					list2 = db2.get(values[1]).getBestSuggestions(prefix);
				} else {
					continue;
				}
				if (db3.containsKey(values[0])) {
					list3 = db3.get(values[0]).getBestSuggestions(prefix);
				} else {
					continue;
				}

				HashMap<String, Integer> tm = new HashMap<String, Integer>();

				AddListToTreeMap(tm, list1);
				AddListToTreeMap(tm, list2);
				AddListToTreeMap(tm, list3);

				TreeMap<Integer, String> res = new TreeMap<Integer, String>();
				for (String s : tm.keySet()) {
					res.put(tm.get(s) * 100 + (int) (Math.random() * 100), s);
				}

				cnt++;
				if (cnt % 250 == 0) {
					System.out.println(cnt + "\t" + values[0] + " " + values[1]
							+ " " + values[2] + " " + prefix
							+ "... \t\t correct term: " + values[3]);
					long end = System.currentTimeMillis();

					if (cnt == 1000) {
						ett = System.currentTimeMillis();
					}
					if (cnt == 3000) {
						dtt = System.currentTimeMillis();
					}
					System.out
							.println("current time " + (end - start) + "\n\n");
					int outk = 0;
					for (Integer w : res.descendingKeySet()) {
						System.out.println(res.get(w) + "\t\t" + w / 100);
						if (++outk > 5) {
							break;
						}
					}
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		long end = System.currentTimeMillis();
		long time = end - start;
		System.out.println(cnt + " saetze in " + time + " ms vorhergesagt ==> "
				+ cnt / Math.max(1, time) + " predictions per millisecond");
		System.out.println("time for 2k secentences without start overhead: "
				+ (dtt - ett) + "==>" + 2000 / Math.max(1, dtt - ett)
				+ " predictions per millisecond");
	}

	private static void AddListToTreeMap(HashMap<String, Integer> tm,
			SuggestionList list) {
		if (tm == null || list == null) {
			return;
		}
		for (int i = 0; i < list.length(); i++) {
			String values[] = list.get(i).split("##");
			if (values.length != 2) {
				break;
			}
			Integer weight = Integer.parseInt(values[1]);
			if (tm.containsKey(values[0])) {
				tm.put(values[0], weight + tm.get(values[0]));
			} else {
				tm.put(values[0], weight);
			}
		}
	}

	private static void OutputList(SuggestionList list) {
		if (list == null) {
			return;
		}
		for (int i = 0; i < list.length(); i++) {
			System.out.println(" " + list.get(i));
		}
	}

	private static void CreateSuggestTree(String word, Node n, int edgeType) {
		Comparator<Double> c = new Comparator<Double>() {
			@Override
			public int compare(Double e1, Double e2) {
				return -Double.compare(e1, e2);
			}
		};
		SuggestTree<Double> st = new SuggestTree<Double>(SUGGESTIONS_COUNT, c);
		HashMap<String, Double> edges = new HashMap<String, Double>();
		for (Relationship rel : n.getRelationships(
				DynamicRelationshipType.withName("rel:" + edgeType),
				Direction.OUTGOING)) {
			Double weight = (Double) rel.getProperty(ConfigHelper.RELLOC_KEY);
			String destWord = (String) rel.getEndNode().getProperty(
					ConfigHelper.NAME_KEY);
			destWord = destWord.concat("##" + (int) (weight * 1000000));
			edges.put(destWord, weight);
		}
		st.build(edges);
		switch (edgeType) {
		case 1:
			db1.put(word, st);
			break;
		case 2:
			db2.put(word, st);
			break;
		case 3:
			db3.put(word, st);
			break;
		}
	}
}
