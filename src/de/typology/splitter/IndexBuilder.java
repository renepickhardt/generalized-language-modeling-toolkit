package de.typology.splitter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public class IndexBuilder {
	private TreeMap<String, Long> wordMap;
	private HashMap<String, String> wordIndex;
	private BufferedReader reader;

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		String dataSet = "wiki/enwiki/";
		String outputDirectory = Config.get().outputDirectory + dataSet;
		IndexBuilder ib = new IndexBuilder();
		ib.buildIndex(outputDirectory + "normalized.txt", outputDirectory
				+ "index.txt");
		// for (Entry<String, String> word : ib.wordIndex.entrySet()) {
		// System.out.println(word.getKey() + " --> " + word.getValue());
		// }
		// ib.wordIndex = null;
		// ib.deserializeIndex(Config.get().outputDirectory + dataSet
		// + "/index.txt");
		// for (Entry<String, String> word : ib.wordIndex.entrySet()) {
		// System.out.println(word.getKey() + " --> " + word.getValue());
		// }
	}

	private void buildMap(String input) {
		IOHelper.log("building word map");
		this.reader = IOHelper.openReadFile(input);
		// declare a comparator for wordMap
		final Comparator<String> comparator = new Comparator<String>() {
			@Override
			public int compare(String s1, String s2) {
				return s1.compareTo(s2);
			}
		};

		this.wordMap = new TreeMap<String, Long>(comparator);
		String line;
		Long lineCount = 0L;
		try {
			while ((line = this.reader.readLine()) != null) {
				lineCount++;
				if (lineCount % 10000 == 0) {
					IOHelper.log("lines: " + lineCount);
				}
				String[] words = line.split("\\s");
				for (String word : words) {
					if (this.wordMap.containsKey(word)) {
						this.wordMap.put(word, this.wordMap.get(word) + 1);
					} else {
						this.wordMap.put(word, 1L);
					}
				}
			}
			this.reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// for (Entry<String, Long> word : this.wordMap.entrySet()) {
		// System.out.println(word.getKey() + " : " + word.getValue());
		// }
	}

	// public HashMap<String, String> buildIndexUgly(String inputPath, String
	// indexPath) {
	// // build WordMap
	// this.buildMap(inputPath);
	//
	// // initialize wordIndex
	// this.wordIndex = new HashMap<String, String>();
	// // read config
	// int maxFiles = Config.get().maxFiles;
	// int minCountPerFile = Config.get().minCountPerFile;
	// // summarize all word counts
	// Long totalCount = 0L;
	// for (Entry<String, Long> word : this.wordMap.entrySet()) {
	// totalCount += word.getValue();
	// }
	// System.out.println("total count: " + totalCount);
	//
	// // calculate max count per file
	// Long maxCountPerFile = totalCount / maxFiles;
	// if (maxCountPerFile < minCountPerFile) {
	// maxCountPerFile = (long) minCountPerFile;
	// }
	//
	// // build index
	// BufferedWriter bw = IOHelper.openWriteFile(indexPath, 1024 * 1024 * 8);
	//
	// Long currentFileCount = 0L;
	// String firstWord = new String();
	// String lastWord = new String();
	// String fileName = new String();
	// ArrayList<String> tempWordList = new ArrayList<String>();
	// Iterator<Map.Entry<String, Long>> wordMapIterator = this.wordMap
	// .entrySet().iterator();
	// try {
	// while (wordMapIterator.hasNext()) {
	// // get next word
	// Entry<String, Long> word = wordMapIterator.next();
	//
	// if (currentFileCount < maxCountPerFile
	// || !wordMapIterator.hasNext()) {
	// if (tempWordList.size() == 0) {
	// // set first word
	// firstWord = word.getKey();
	// }
	// // store word in tempWordList (to be able to index the
	// // correct
	// // file name)
	// tempWordList.add(word.getKey());
	// currentFileCount += word.getValue();
	// }
	// if (currentFileCount >= maxCountPerFile
	// || !wordMapIterator.hasNext()) {
	// if (tempWordList.size() < 0) {
	// IOHelper.strongLog("buildIndex() was trying to write an empty file into index...abort");
	// return null;
	// }
	// // set last word
	// lastWord = tempWordList.get(tempWordList.size() - 1);
	// // set file name
	// fileName = firstWord + "_" + lastWord;
	// // store words in wordIndex and index file
	// for (String tempWord : tempWordList) {
	// this.wordIndex.put(tempWord, fileName);
	// bw.write(tempWord + "\t" + fileName + "\n");
	// }
	// // reset for next file
	// tempWordList = new ArrayList<String>();
	// currentFileCount = 0L;
	// }
	// }
	// bw.flush();
	// bw.close();
	// } catch (IOException e) {
	// File indexFile = new File(fileName);
	// if (indexFile.exists()) {
	// indexFile.delete();
	// }
	// e.printStackTrace();
	// }
	// return this.wordIndex;
	// }

	public HashMap<String, String> buildIndex(String inputPath, String indexPath) {

		// build WordMap
		this.buildMap(inputPath);

		IOHelper.log("building word index");
		// initialize wordIndex
		this.wordIndex = new HashMap<String, String>();
		// read config
		int maxFiles = Config.get().maxFiles;
		int minCountPerFile = Config.get().minCountPerFile;
		// summarize all word counts
		Long totalCount = 0L;
		for (Entry<String, Long> word : this.wordMap.entrySet()) {
			totalCount += word.getValue();
		}
		System.out.println("total count: " + totalCount);

		// calculate max count per file
		Long maxCountPerFile = totalCount / maxFiles;
		if (maxCountPerFile < minCountPerFile) {
			maxCountPerFile = (long) minCountPerFile;
		}

		// build index
		BufferedWriter bw = IOHelper.openWriteFile(indexPath, 1024 * 1024 * 8);
		Integer fileCount = 0;
		Long currentFileCount = 0L;
		ArrayList<String> tempWordList = new ArrayList<String>();
		Iterator<Map.Entry<String, Long>> wordMapIterator = this.wordMap
				.entrySet().iterator();
		try {
			while (wordMapIterator.hasNext()) {
				// get next word
				Entry<String, Long> word = wordMapIterator.next();

				if (currentFileCount < maxCountPerFile
						|| !wordMapIterator.hasNext()) {
					// store word in tempWordList (to be able to index the
					// correct
					// file name)
					tempWordList.add(word.getKey());
					currentFileCount += word.getValue();
				}
				if (currentFileCount >= maxCountPerFile
						|| !wordMapIterator.hasNext()) {
					if (tempWordList.size() < 0) {
						IOHelper.strongLog("buildIndex() was trying to write an empty file into index...abort");
						return null;
					}
					// store words in wordIndex and index file
					for (String tempWord : tempWordList) {
						this.wordIndex.put(tempWord, fileCount.toString());
						bw.write(tempWord + "\t" + fileCount + "\n");
					}
					// reset for next file
					tempWordList = new ArrayList<String>();
					currentFileCount = 0L;
					fileCount++;
				}
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			File indexFile = new File(indexPath);
			if (indexFile.exists()) {
				indexFile.delete();
			}
			e.printStackTrace();
		}
		return this.wordIndex;
	}

	public HashMap<String, String> deserializeIndex(String indexPath) {
		IOHelper.log("deserializing word index");
		this.wordIndex = new HashMap<String, String>();
		try {
			BufferedReader br = IOHelper.openReadFile(indexPath,
					1024 * 1024 * 8);
			String line;
			String[] lineSplit;
			while ((line = br.readLine()) != null) {
				lineSplit = line.split(" ");
				this.wordIndex.put(lineSplit[0], lineSplit[1]);
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return this.wordIndex;
	}
}
