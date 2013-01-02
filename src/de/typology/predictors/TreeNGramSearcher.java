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

public class TreeNGramSearcher extends TreeSearcher{
	public TreeNGramSearcher(int n, int k, int joinLength,HashMap <Integer,HashMap<String,SuggestTree<Float>>> treeMapMap) {
		super(n, k, joinLength);
		TreeNGramSearcher.treeMapMap=treeMapMap;

	}

	protected static HashMap <Integer,HashMap<String,SuggestTree<Float>>> treeMapMap;
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		TreeIndexer tti=new TreeIndexer();
		HashMap <Integer,HashMap<String,SuggestTree<Float>>> treeMapMap= tti.run(Config.get().normalizedNGrams);
		TreeNGramSearcher tns=new TreeNGramSearcher(5, 5, 12,treeMapMap);
		tns.query("1991 1992 auf 1994", "a", "aus");
		HashMap<String, Float>result=tns.search("1991 1992 auf 1994", "a","");
		for(Entry<String, Float> e:result.entrySet()){
			System.out.println(e.getKey()+" "+e.getValue());
		}
	}



	@Override
	public HashMap<String, Float> search(String q, String prefix, String match) {
		HashMap<String, Float> result = new HashMap<String, Float>();
		ArrayList<SuggestTree<Float>> trees;
		String[] terms = this.getQueryNGrams(q);
		if (terms == null) {
			return null;
		}

		for (int i = 0; i < terms.length; i++) {
			trees=new ArrayList<SuggestTree<Float>>();
			//get list of trees
			String t1=terms[0].substring(0, 1);
			String t2=terms[1].substring(0, 1);
			if(prefix.length()!=0){
				if(treeMapMap.get(i+2).containsKey(t1)){
					trees.add(treeMapMap.get(i+2).get(t1));
				}else{
					if(treeMapMap.get(i+2).containsKey(t1+t2)){
						trees.add(treeMapMap.get(i+2).get(t1+t2));
					}else{
						if(treeMapMap.get(i+2).containsKey(t1+"other")){
							trees.add(treeMapMap.get(i+2).get(t1+"other"));
						}else{
							trees.add(treeMapMap.get(i+2).get("other"));
						}
					}
				}
			}

			float weight=1;
			for(SuggestTree<Float> tree:trees){
				Node<Float> node=tree.getBestSuggestions(terms[i]+" "+prefix);
				if(node!=null){
					for(int join=0;join<this.joinLength&&join<node.listLength();join++){
						Pair<Float> pair=node.getSuggestion(join);
						String[] split=pair.getString().split(" ");
						String key=split[split.length-1];
						Float value=pair.getScore();
						if (result.containsKey(key)) {
							result.put(key, weight * value + result.get(key));
						} else {
							result.put(key, weight * value);
						}
					}
				}
			}
		}

		return result;
	}

	public String[] getQueryNGrams(String q) {
		String[] words = q.split(" ");
		int l = words.length - 1;
		if (words.length == 1) {
			String[] result = new String[1];
			result[0] = words[l];
			return result;
		} else if (words.length == 2) {
			String[] result = new String[2];
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


	@Override
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
