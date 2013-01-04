package de.typology.predictors;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import de.typology.trainers.SuggestTree;
import de.typology.trainers.SuggestTree.Node;
import de.typology.trainers.SuggestTree.Pair;
import de.typology.trainers.TreeIndexer;
import de.typology.utils.Config;

public class TreeTypologySearcher extends TreeSearcher {
	public TreeTypologySearcher(int n, int k, int joinLength) {
		super(n, k, joinLength);

	}

	// / protected static HashMap<Integer, HashMap<String, SuggestTree<Float>>>
	// TreeIndexer.treeMapMap;

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		TreeIndexer tti = new TreeIndexer();
		tti.run(Config.get().normalizedEdges);
		TreeTypologySearcher tts = new TreeTypologySearcher(5, 5, 12);
		tts.query("1991 1992 1993 1994", "a", "als");
		HashMap<String, Float> result = tts.search("1991 1992 1993 1994", "a",
				"");
		for (Entry<String, Float> e : result.entrySet()) {
			System.out.println(e.getKey() + " " + e.getValue());
		}

	}

	@Override
	public HashMap<String, Float> search(String q, String prefix, String match) {
		HashMap<String, Float> result = new HashMap<String, Float>();
		ArrayList<SuggestTree<Float>> trees;
		String[] terms = q.split(" ");

		int edge = 1;
		for (int i = terms.length - 1; i >= Math.max(0, terms.length - 4); i--) {
			trees = new ArrayList<SuggestTree<Float>>();
			// get list of trees
			String t1 = terms[i].substring(0, 1);
			if (prefix.length() != 0) {
				if (TreeIndexer.treeMapMap.get(edge).containsKey(t1)) {
					trees.add(TreeIndexer.treeMapMap.get(edge).get(t1));
				} else {
					String p1 = prefix.substring(0, 1);
					if (TreeIndexer.treeMapMap.get(edge).containsKey(t1 + p1)) {
						trees.add(TreeIndexer.treeMapMap.get(edge).get(t1 + p1));
					} else {
						if (TreeIndexer.treeMapMap.get(edge).containsKey(
								t1 + "other")) {
							trees.add(TreeIndexer.treeMapMap.get(edge).get(
									t1 + "other"));
						} else {
							trees.add(TreeIndexer.treeMapMap.get(edge).get(
									"other"));
						}
					}
				}
			} else {
				for (Entry<String, SuggestTree<Float>> entry : TreeIndexer.treeMapMap
						.get(edge).entrySet()) {
					if (entry.getKey().startsWith(t1)) {
						trees.add(entry.getValue());
					}
				}
			}

			float weight = 1;
			for (SuggestTree<Float> tree : trees) {
				Node<Float> node = tree.getBestSuggestions(terms[i] + " "
						+ prefix);
				if (node != null) {
					for (int join = 0; join < this.joinLength
							&& join < node.listLength(); join++) {
						Pair<Float> pair = node.getSuggestion(join);
						String key = pair.getString().split(" ")[1];
						Float value = pair.getScore();
						if (result.containsKey(key)) {
							result.put(key, weight * value + result.get(key));
						} else {
							result.put(key, weight * value);
						}
					}
				}
			}
			edge++;
		}

		return result;
	}

	@Override
	public String getFileName() {
		String name = "";
		if (Config.get().useWeights) {
			name = name.concat("weighted-");
		}
		name = name.concat("typo-" + this.n + "-joinLengh-" + this.joinLength
				+ "-" + Config.get().sampleRate + Config.get().splitDataRatio
				+ ".log");
		// TODO Auto-generated method stub
		return name;
	}

}
