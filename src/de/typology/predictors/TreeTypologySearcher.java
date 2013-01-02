package de.typology.predictors;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

import de.typology.trainers.SuggestTree;
import de.typology.trainers.SuggestTree.Node;
import de.typology.trainers.SuggestTree.Pair;
import de.typology.trainers.TreeTypologyIndexer;
import de.typology.utils.Config;

public class TreeTypologySearcher {
	protected static HashMap <Integer,HashMap<String,SuggestTree<Float>>> treeMapMap;
	protected int joinLength;
	protected int k;
	protected int n;
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		TreeTypologyIndexer tti=new TreeTypologyIndexer();
		treeMapMap=tti.run(Config.get().normalizedEdges);
		TreeTypologySearcher tts=new TreeTypologySearcher(5, 5, 12);
		HashMap<String, Float>result=tts.search("1 12 123 1991", "a","");
		for(Entry<String, Float> e:result.entrySet()){
			System.out.println(e.getKey()+" "+e.getValue());
		}

	}
	public TreeTypologySearcher(int n, int k, int joinLength)   {
		this.k = k;
		this.n = n;
		this.joinLength = joinLength;
	}


	public HashMap<String, Float> search(String q, String prefix, String match) {
		HashMap<String, Float> result = new HashMap<String, Float>();

		String[] terms = q.split(" ");
		int edge = 1;

		for (int i = terms.length - 1; i >= Math.max(0, terms.length - 4); i--) {
			float weight=1;
			Node<Float> node=treeMapMap.get(edge).get("1a").getBestSuggestions(terms[i]+" "+prefix);
			for(int join=0;join<=this.joinLength;join++){
				if(node!=null){
					Pair<Float> pair=node.getSuggestion(i);
					String key=pair.getString();
					Float value=pair.getScore();
					if (result.containsKey(key)) {
						result.put(key, weight * value + result.get(key));
					} else {
						result.put(key, weight * value);
					}
				}
			}
			edge++;
		}

		return result;
	}


	public String getFileName() {
		String name = "";
		if (Config.get().useWeights) {
			name = name.concat("weighted-");
		}
		name = name.concat("typo-" + this.n+ "-joinLengh-" + this.joinLength
				+ "-" + Config.get().sampleRate + Config.get().splitDataRatio
				+ ".log");
		// TODO Auto-generated method stub
		return name;
	}
}
