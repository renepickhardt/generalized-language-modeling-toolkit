package de.typology.splitter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public class IndexBuilder {
	private TreeMap<String, Long> wordMap;
	private String[] wordIndex;
	private BufferedReader reader;
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
		String dataSet = "wiki/barwiki/";
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

		this.wordMap = new TreeMap<String, Long>(this.comparator);
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

					// cut down word to make wordMap smaller
					if (word.length() > 2) {
						word = word.substring(0, 1);
					}

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

	public void buildIndex(String inputPath, String indexPath) {

		// build WordMap
		this.buildMap(inputPath);

		IOHelper.log("building word index");
		// read config
		int maxFiles = Config.get().maxFiles;
		int minCountPerFile = Config.get().minCountPerFile;
		// summarize all word counts
		Long totalCount = 0L;
		for (Entry<String, Long> word : this.wordMap.entrySet()) {
			totalCount += word.getValue();
		}
		IOHelper.log("total words count: " + totalCount);

		// calculate max count per file
		Long maxCountPerFile = totalCount / maxFiles;
		if (maxCountPerFile < minCountPerFile) {
			maxCountPerFile = (long) minCountPerFile;
		}

		// build index
		BufferedWriter bw = IOHelper.openWriteFile(indexPath, 1024 * 1024 * 8);
		Long currentFileCount = 0L;
		int fileCount = 0;
		Iterator<Map.Entry<String, Long>> wordMapIterator = this.wordMap
				.entrySet().iterator();
		try {
			while (wordMapIterator.hasNext()) {
				// get next word
				Entry<String, Long> word = wordMapIterator.next();
				currentFileCount += word.getValue();
				if (currentFileCount >= maxCountPerFile) {
					bw.write(word.getKey() + "\t" + fileCount + "\n");
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
		return;
	}

	public String[] deserializeIndex(String indexPath) {
		IOHelper.log("deserializing word index");
		// count total number of lines
		int lineCount = 0;
		try {
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
