package de.typology.indexer;

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

	File indexFile;

	public WordIndexer(File indexFile) {
		this.indexFile = indexFile;
	}

	private TreeMap<String, Long> buildMap(File InputFile) {
		// TODO:fix initialize
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

	public void buildIndex(File inputFile, int maxCountDivider,
			int minCountPerFile) {
		// build WordMap
		TreeMap<String, Long> wordMap = this.buildMap(inputFile);

		// summarize all word counts
		Long totalCount = 0L;
		for (Entry<String, Long> word : wordMap.entrySet()) {
			totalCount += word.getValue();
		}

		// calculate max count per file
		Long maxCountPerFile = totalCount / maxCountDivider;
		System.out.println(maxCountPerFile);
		if (maxCountPerFile < minCountPerFile) {
			maxCountPerFile = (long) minCountPerFile;
		}

		// build index
		BufferedWriter indexWriter;
		try {
			indexWriter = new BufferedWriter(new FileWriter(this.indexFile));
			Long currentFileCount = 0L;
			int fileCount = 0;
			Iterator<Map.Entry<String, Long>> wordMapIterator = wordMap
					.entrySet().iterator();
			Entry<String, Long> word;
			// set first file
			if (wordMapIterator.hasNext()) {
				word = wordMapIterator.next();
				indexWriter.write(word.getKey() + "\t" + fileCount + "\n");
				currentFileCount = word.getValue();
				fileCount++;
			}
			while (wordMapIterator.hasNext()) {
				// get next word
				word = wordMapIterator.next();
				if (currentFileCount + word.getValue() > maxCountPerFile) {
					indexWriter.write(word.getKey() + "\t" + fileCount + "\n");
					currentFileCount = word.getValue();
					fileCount++;
				} else {
					currentFileCount += word.getValue();
				}
			}
			indexWriter.flush();
			indexWriter.close();
		} catch (IOException e) {
			if (this.indexFile.exists()) {
				this.indexFile.delete();
			}
			e.printStackTrace();
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return;
	}

	public String[] getIndex() {
		// count total number of lines
		int lineCount = 0;
		try {
			BufferedReader br = new BufferedReader(new FileReader(
					this.indexFile));
			while (br.readLine() != null) {
				lineCount++;
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		String[] index = new String[lineCount];
		int currentLineCount = 0;
		try {
			BufferedReader br = new BufferedReader(new FileReader(
					this.indexFile));
			String line;
			String[] lineSplit;
			while ((line = br.readLine()) != null) {
				lineSplit = line.split("\t");
				index[currentLineCount] = lineSplit[0];
				currentLineCount++;
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return index;
	}

}
