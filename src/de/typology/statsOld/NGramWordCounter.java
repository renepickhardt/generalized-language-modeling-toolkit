package de.typology.statsOld;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;

import de.typology.utils.Config;
import de.typology.utilsOld.IOHelper;

public class NGramWordCounter {
	private static ArrayList<File> files;
	private BufferedReader reader;
	private BufferedWriter wordsWriter;
	private BufferedWriter statsWriter;
	private BufferedWriter wordsDistributionWriter;
	private HashMap<String, Integer> wordMap;
	private TreeMap<String, Integer> sortedWordMap;
	private String line;
	private long wordCount;

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {

		files = IOHelper.getDirectory(new File(Config.get().wordCountInput));
		String filePathCut = new File(Config.get().wordCountInput).getParent();
		long wordCount=0;
		for (File file : files) {
			long startTime = System.currentTimeMillis();
			long endTime = 0;
			long sek = 0;
			NGramWordCounter wC = new NGramWordCounter(file.getAbsolutePath(),
					filePathCut + "words.txt", filePathCut
					+ "wordsdistribution.txt",
					Config.get().wordCountStats);
			System.out.println(file.getAbsolutePath() + ": ");
			System.out.println("start counting");
			wordCount+=wC.countWords();
			//			System.out.println("counting done, start sorting");
			//			wC.sortWordMap();
			//			System.out.println("sorting done, start writing to file");
			//			wC.printWordsAndDistribution();
			endTime = System.currentTimeMillis();
			sek = (endTime - startTime) / 1000;
			//	wC.printStats(file, sek);
			System.out.println("done");
			System.out.println("unique words: "+wordCount);
		}
	}

	public NGramWordCounter(String input, String wordsOutput,
			String wordsDistributionOutput, String statsOutput)
					throws IOException {
		this.reader = IOHelper.openReadFile(input);
		this.wordsWriter = IOHelper.openWriteFile(wordsOutput);
		this.wordsDistributionWriter = IOHelper
				.openWriteFile(wordsDistributionOutput);
		this.statsWriter = new BufferedWriter(new FileWriter(statsOutput, true));
	}

	private void printStats(File file, long sek) throws IOException {

		this.statsWriter.write(file.getAbsolutePath() + ":" + "\n");
		this.statsWriter.write("\t" + "total words: " + this.wordCount + "\n");
		this.statsWriter.write("\t" + "size in bytes: " + file.length() + "\n");
		this.statsWriter.write("\t" + "average size of one word in bytes: "
				+ file.length() / this.wordCount + "\n");
		this.statsWriter.write("\t" + "unique words: " + this.wordMap.size()
				+ "\n");
		this.statsWriter.write("\t" + "seconds to generate stats: " + sek
				+ "\n");
		Date date = new Date();
		this.statsWriter.write("\t" + "date: " + date + "\n");
		this.statsWriter.flush();
		this.statsWriter.close();
	}

	private final Comparator<String> StringComparator = new Comparator<String>() {
		@Override
		public int compare(String a, String b) {
			if (NGramWordCounter.this.wordMap.get(a) >= NGramWordCounter.this.wordMap
					.get(b)) {
				return -1;
			} else {
				return 1;
			} // returning 0 would merge keys
		}
	};

	public long countWords() throws IOException {
		this.wordMap = new HashMap<String, Integer>();

		while ((this.line = this.reader.readLine()) != null) {
			String[] splitLine = this.line.split("\t");
			String[] words = Arrays.copyOfRange(splitLine, 0,
					splitLine.length - 1);
			String countWithRhomb = splitLine[splitLine.length - 1];
			if(countWithRhomb.length()>1){
				int count = Integer.parseInt(countWithRhomb.substring(1,
						countWithRhomb.length()));
				for (String word : words) {
					if (this.wordMap.containsKey(word)) {
						this.wordMap.put(word, this.wordMap.get(word) + count);
					} else {
						this.wordMap.put(word, count);
					}
				}
			}
		}
		this.reader.close();
		return this.wordMap.size();
	}

	public void sortWordMap() {
		this.sortedWordMap = new TreeMap<String, Integer>(this.StringComparator);
		this.sortedWordMap.putAll(this.wordMap);
	}

	private void printWordsAndDistribution() throws IOException {
		Entry<String, Integer> entry;
		int currentOccurrences;
		int currentOccurrencesCount;
		// first entry has to be handled separately due to initializing
		// occurrences
		if ((entry = this.sortedWordMap.pollFirstEntry()) != null) {
			currentOccurrences = entry.getValue();
			currentOccurrencesCount = 1;
			this.wordsWriter.write(entry.getKey() + "#" + entry.getValue()
					+ "\n");
			this.wordsWriter.flush();
			this.wordCount += entry.getValue();
			// for (int i = 0; i < 1000; i++) {
			while (true) {
				if ((entry = this.sortedWordMap.pollFirstEntry()) != null) {
					// map is not empty
					if (currentOccurrences == entry.getValue()) {
						currentOccurrencesCount++;
					} else {
						this.wordsDistributionWriter.write(currentOccurrences
								+ "#" + currentOccurrencesCount + "\n");
						this.wordsDistributionWriter.flush();
						currentOccurrences = entry.getValue();
						currentOccurrencesCount = 1;
					}
					this.wordCount += entry.getValue();
					this.wordsWriter.write(entry.getKey() + "#"
							+ entry.getValue() + "\n");
					this.wordsWriter.flush();
				} else {
					// map is empty
					this.wordsDistributionWriter.write(currentOccurrences + "#"
							+ currentOccurrencesCount + "\n");
					this.wordsDistributionWriter.flush();
					break;
				}
			}
		}
		this.wordsWriter.close();
		this.wordsDistributionWriter.close();
	}
}
