package de.typology.trainers;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

import de.typology.trainers.SuggestTree.Node;
import de.typology.trainers.SuggestTree.Pair;
import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public class TreeIndexer {
	private BufferedReader reader;
	private int joinLength = 6;
	private String line;
	private String[] lineSplit;
	private Float edgeCount;
	private static HashMap <Integer,HashMap<String,SuggestTree<Float>>> treeMapMap;
	private HashMap <String,SuggestTree<Float>> treeMap;
	private HashMap <String,Float> edgeMap;
	private Comparator<Float> comparator;

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {	
		TreeIndexer tti=new TreeIndexer();
		tti.run(Config.get().normalizedEdges);
		Node<Float> node=treeMapMap.get(1).get("1a").getBestSuggestions("1991" + " a");
		for(int i=0;i<5;i++){
			Pair<Float> pair=node.getSuggestion(i);
			System.out.println(i+": "+pair.getString()+" score: "+pair.getScore());
		}
		tti.run(Config.get().normalizedNGrams);
		node=treeMapMap.get(3).get("1a").getBestSuggestions("1991 an" + " d");
		for(int i=0;i<5&&i<node.listLength();i++){
			Pair<Float> pair=node.getSuggestion(i);
			System.out.println(i+": "+pair.getString()+" score: "+pair.getScore());
		}
		int mb = 1024*1024;
		Runtime runtime = Runtime.getRuntime();
		System.out.println("Used Memory:"
				+ (runtime.totalMemory() - runtime.freeMemory()) / mb);
	}
	public HashMap <Integer,HashMap<String,SuggestTree<Float>>>  run(String normalizedTypologyEdgesPath) throws IOException  {
		long startTime = System.currentTimeMillis();
		treeMapMap=new HashMap<Integer, HashMap<String,SuggestTree<Float>>>();
		for (int edgeType = 1; edgeType < 6; edgeType++) {
			TreeIndexer indexer = new TreeIndexer();
			indexer.index(normalizedTypologyEdgesPath + edgeType + "/",edgeType);
		}
		long endTime = System.currentTimeMillis();
		IOHelper.strongLog((endTime - startTime) / 1000
				+ " seconds for indexing " + normalizedTypologyEdgesPath);
		return treeMapMap;
	}
	public TreeIndexer()  {
	}

	private int index(String dataDir,Integer edgeType) throws IOException {
		this.treeMap=new HashMap<String, SuggestTree<Float>>();
		treeMapMap.put(edgeType, this.treeMap);
		this.comparator=new Comparator<Float>() {

			@Override
			public int compare(Float o1, Float o2) {
				return o1.compareTo(o2);

			}
		};
		ArrayList<File> files = IOHelper.getDirectory(new File(dataDir));
		for (File file : files) {
			if (file.getName().contains("distribution")) {
				IOHelper.log("skipping " + file.getAbsolutePath());
				continue;
			}
			IOHelper.log("indexing " + file.getAbsolutePath());
			this.indexFile(file);
		}
		return files.size();
	}
	private int indexFile(File file) throws IOException {
		file.getTotalSpace();
		this.reader = IOHelper.openReadFile(file.getAbsolutePath());
		this.edgeMap=new HashMap<String, Float>();
		int docCount = 0;
		while ((this.line = this.reader.readLine()) != null) {
			this.lineSplit = this.line.split("\t#");
			if (this.lineSplit.length != 2) {
				IOHelper.strongLog("can;t index line split is incorrectly"
						+ this.lineSplit.length);
				continue;
			}
			this.edgeCount = Float
					.parseFloat(this.lineSplit[this.lineSplit.length - 1]);
			String tmp = this.lineSplit[0].replace('\t', ' ');

			this.edgeMap.put(tmp, this.edgeCount);
			docCount++;
		}
		SuggestTree<Float> tree=new SuggestTree<Float>();

		tree.build(this.edgeMap, this.comparator, this.joinLength);
		this.treeMap.put(file.getName().split("\\.")[0], tree);
		return docCount;
	}

}
