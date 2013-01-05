package de.typology.predictors;

import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import de.typology.trainers.SuggestTree;
import de.typology.trainers.TreeIndexer;
import de.typology.trainers.TreeServerIndexer;
import de.typology.trainers.TreeServerInterface;
import de.typology.utils.Config;

public class TreeServerTypologySearcher extends TreeSearcher {
	public TreeServerTypologySearcher(int n, int k, int joinLength) {
		super(n, k, joinLength);

	}

	// / protected static HashMap<Integer, HashMap<String, SuggestTree<Float>>>
	// TreeIndexer.treeMapMap;

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		TreeServerIndexer tsi = new TreeServerIndexer();
		tsi.run(Config.get().normalizedEdges);
		TreeServerTypologySearcher tts = new TreeServerTypologySearcher(5, 5, 12);
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
		ArrayList<String> trees;
		TreeServerInterface server;
		String[] terms = q.split(" ");

		int edge = 1;
		for (int i = terms.length - 1; i >= Math.max(0, terms.length - 4); i--) {
			trees = new ArrayList<String>();
			// get list of trees
			String t1 = terms[i].substring(0, 1);
			if (prefix.length() != 0) {
				if (TreeServerIndexer.treeMap.contains(t1+"."+edge+"es")) {
					trees.add(t1+"."+edge+"es");
				} else {
					String p1 = prefix.substring(0, 1);
					if (TreeServerIndexer.treeMap.contains(t1 + p1+"."+edge+"es")) {
						trees.add(t1 + p1+"."+edge+"es");
					} else {
						if (TreeServerIndexer.treeMap.contains(
								t1 + "other."+edge+"es")) {
							trees.add(t1 + "other."+edge+"es");
						} else {
							trees.add("other."+edge+"es");
						}
					}
				}
			} else {
				for (Entry<String, SuggestTree<Float>> entry : TreeIndexer.treeMapMap
						.get(edge).entrySet()) {
					if (entry.getKey().startsWith(t1)) {
						trees.add(entry.getKey()+"."+edge+"es");
					}
				}
			}

			float weight = 1;
			for (String tree : trees) {
				try {
					server = (TreeServerInterface)Naming.lookup("//127.0.0.1/"+tree);
					result.putAll(server.getBestSuggestions(terms[i] + " "
							+ prefix,weight));
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (NotBoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			edge++;
		}
		return result;
		//		//cut down result list
		//		HashMap<String, Float> resultCut = new HashMap<String, Float>();
		//		ValueComparator bvc =  new ValueComparator(result);
		//		TreeMap<String,Float> resultTop=new TreeMap<String, Float>(bvc);
		//		for(Entry<String, Float> e: result.entrySet()){
		//			resultTop.put(e.getKey(), e.getValue());
		//		}
		//		for(int join=0;join<12&&join<resultTop.size();join++) {
		//			Entry<String, Float> e=resultTop.pollFirstEntry();
		//			resultCut.put(e.getKey(),e.getValue());
		//		}
		//		return resultCut;
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
