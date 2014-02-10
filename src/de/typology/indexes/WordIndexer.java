package de.typology.indexes;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * A class for building a text file containing a index representation for a
 * given text file based on the alphabetical distribution of its words.
 * 
 * @author Martin Koerner
 * 
 */
public class WordIndexer {

	private TreeMap<String, Long> buildMap(File InputFile,
			String addBeforeSentence, String addAfterSentence) {
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(InputFile));
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return null;
		}

		// a comparator for wordMap
		Comparator<String> StringComparator = new Comparator<String>() {
			@Override
			public int compare(String s1, String s2) {
				return s1.compareTo(s2);
			}
		};

		TreeMap<String, Long> wordMap = new TreeMap<String, Long>(
				StringComparator);
		String line;
		// long lineCount=0L;
		try {
			while ((line = reader.readLine()) != null) {
				line = addBeforeSentence + line + addAfterSentence;
				// lineCount++;
				// IOHelper.log("");
				// IOHelper.log("lines: " + lineCount);
				//
				// int mb = 1024 * 1024;
				//
				// // Getting the runtime reference from system
				// Runtime runtime = Runtime.getRuntime();
				//
				// IOHelper.log("##### Heap utilization statistics [MB] #####");
				//
				// // Print used memory
				// IOHelper.log("Used Memory:\t"
				// + (runtime.totalMemory() - runtime.freeMemory())
				// / mb);
				//
				// // Print free memory
				// IOHelper.log("Free Memory:\t" + runtime.freeMemory() / mb);
				//
				// // Print total available memory
				// IOHelper.log("Total Memory:\t" + runtime.totalMemory() / mb);
				//
				// // Print Maximum available memory
				// IOHelper.log("Max Memory:\t" + runtime.maxMemory() / mb);
				String[] words = line.split("\\s+");
				for (String word : words) {
					if (wordMap.containsKey(word)) {
						wordMap.put(word, wordMap.get(word) + 1);
					} else {
						wordMap.put(word, 1L);
					}
				}
			}
			reader.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
		// for (Entry<String, Long> word : this.wordMap.entrySet()) {
		// System.out.println(word.getKey() + " : " + word.getValue());
		// }
		return wordMap;
	}

	/**
	 * 
	 * @param inputFile
	 * @param maxCountDivider
	 * @return Long: maxCountPerFile
	 */
	public long buildIndex(File inputFile, File indexOutputFile,
			int maxCountDivider, String addBeforeSentence,
			String addAfterSentence) {

		// build WordMap
		TreeMap<String, Long> wordMap = this.buildMap(inputFile,
				addBeforeSentence, addAfterSentence);

		// summarize all word counts
		Long totalCount = 0L;
		for (Entry<String, Long> word : wordMap.entrySet()) {
			totalCount += word.getValue();
		}

		// calculate max count per file
		Long maxCountPerFile = totalCount / maxCountDivider;
		// System.out.println("maxCountPerFile: " + maxCountPerFile);
		if (maxCountPerFile < 1L) {
			maxCountPerFile = 1L;
		}

		// build index
		BufferedWriter indexWriter;
		try {
			indexWriter = new BufferedWriter(new FileWriter(indexOutputFile));
			Long currentFileCount = 0L;
			int fileCount = 0;
			Iterator<Map.Entry<String, Long>> wordMapIterator = wordMap
					.entrySet().iterator();
			Entry<String, Long> word;

			while (wordMapIterator.hasNext()) {
				// get next word
				word = wordMapIterator.next();
				if (fileCount == 0
						|| currentFileCount + word.getValue() > maxCountPerFile) {
					indexWriter.write(word.getKey() + "\t" + fileCount + "\n");
					currentFileCount = word.getValue();
					fileCount++;
				} else {
					currentFileCount += word.getValue();
				}
			}
			indexWriter.close();
		} catch (IOException e) {
			// make sure that no corrupted index file is stored
			if (indexOutputFile.exists()) {
				indexOutputFile.delete();
			}
			e.printStackTrace();
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return maxCountPerFile;
	}

}
