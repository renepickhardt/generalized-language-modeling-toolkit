package de.typology.splitterOld;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import de.typology.utilsOld.Config;
import de.typology.utilsOld.IOHelper;

public class IndexBuilder {
	private TreeMap<String, Long> wordMap;
	private String[] wordIndex;
	private final Comparator<String> comparator = new Comparator<String>() {
		@Override
		public int compare(String s1, String s2) {
			return s1.compareTo(s2);
		}
	};

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		String outputDirectory = Config.get().outputDirectory
				+ Config.get().inputDataSet;
		IndexBuilder ib = new IndexBuilder();
		ib.buildIndex(outputDirectory + "normalized.txt", outputDirectory
				+ "index.txt", outputDirectory + "stats.txt");
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
		BufferedReader reader = IOHelper.openReadFile(input);
		// declare a comparator for wordMap

		this.wordMap = new TreeMap<String, Long>(this.comparator);
		String line;
		Long lineCount = 0L;
		try {
			while ((line = reader.readLine()) != null) {
				if (Config.get().addSentenceTags) {
					line = "<s> " + line + " </s>";
					if (Config.get().addFakeStartTag) {
						line = "<fs> " + line;
					}
				}
				lineCount++;
				if (lineCount % 10000 == 0) {
					IOHelper.log("");
					IOHelper.log("lines: " + lineCount);

					int mb = 1024 * 1024;

					// Getting the runtime reference from system
					Runtime runtime = Runtime.getRuntime();

					IOHelper.log("##### Heap utilization statistics [MB] #####");

					// Print used memory
					IOHelper.log("Used Memory:\t"
							+ (runtime.totalMemory() - runtime.freeMemory())
							/ mb);

					// Print free memory
					IOHelper.log("Free Memory:\t" + runtime.freeMemory() / mb);

					// Print total available memory
					IOHelper.log("Total Memory:\t" + runtime.totalMemory() / mb);

					// Print Maximum available memory
					IOHelper.log("Max Memory:\t" + runtime.maxMemory() / mb);
				}
				String[] words = line.split("\\s+");
				for (String word : words) {

					// cut down word to make wordMap smaller
					// if (word.length() > 3) {
					// word = word.substring(0, 2);
					// }

					if (this.wordMap.containsKey(word)) {
						this.wordMap.put(word, this.wordMap.get(word) + 1);
					} else {
						this.wordMap.put(word, 1L);
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
	}

	public void buildIndex(String inputPath, String indexPath, String statsPath) {

		// build WordMap
		this.buildMap(inputPath);

		IOHelper.strongLog("building word index");
		// read config
		int maxCountDivider = Config.get().maxCountDivider;
		int minCountPerFile = Config.get().minCountPerFile;
		// summarize all word counts
		Long totalCount = 0L;
		for (Entry<String, Long> word : this.wordMap.entrySet()) {
			totalCount += word.getValue();
		}
		// write stats

		BufferedWriter statsWriter = IOHelper.openWriteFile(statsPath);
		try {
			statsWriter.write(inputPath + ":\n");
			statsWriter.write("total words: " + totalCount + "\n");
			statsWriter.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		IOHelper.log("total words: " + totalCount);

		// calculate max count per file (+1 to always round up)
		Long maxCountPerFile = totalCount / maxCountDivider;
		System.out.println(maxCountPerFile);
		if (maxCountPerFile < minCountPerFile) {
			maxCountPerFile = (long) minCountPerFile;
		}

		// build index
		BufferedWriter indexWriter = IOHelper.openWriteFile(indexPath);
		Long currentFileCount = 0L;
		int fileCount = 0;
		Iterator<Map.Entry<String, Long>> wordMapIterator = this.wordMap
				.entrySet().iterator();
		try {
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
			File indexFile = new File(indexPath);
			if (indexFile.exists()) {
				indexFile.delete();
			}
			e.printStackTrace();
		}
		return;
	}

	public String[] deserializeIndex(String indexPath) {
		IOHelper.strongLog("deserializing word index");
		// count total number of lines
		int lineCount = 0;
		try {
			System.out.println(indexPath);
			BufferedReader br = IOHelper.openReadFile(indexPath,
					1024 * 1024 * 8);
			while (br.readLine() != null) {
				lineCount++;
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		this.wordIndex = new String[lineCount];
		int currentLineCount = 0;
		try {
			BufferedReader br = IOHelper.openReadFile(indexPath,
					1024 * 1024 * 8);
			String line;
			String[] lineSplit;
			while ((line = br.readLine()) != null) {
				lineSplit = line.split("\t");
				this.wordIndex[currentLineCount] = lineSplit[0];
				currentLineCount++;
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return this.wordIndex;
	}
}
