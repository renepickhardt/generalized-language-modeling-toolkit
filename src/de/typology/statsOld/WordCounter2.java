package de.typology.statsOld;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.HashSet;

import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public class WordCounter2 {
	private BufferedReader reader;
	private BufferedWriter wordsWriter;
	private BufferedWriter statsWriter;
	private BufferedWriter wordsDistributionWriter;
	private HashSet<String> wordSet;
	//private TreeMap<String, Integer> sortedWordMap;
	private String line;
	private long wordCount;
	private long wordCountCheck;
	private long uniqueWords;

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {

		long startTime = System.currentTimeMillis();
		long endTime = 0;
		long sek = 0;

		String filePathCut = new File(Config.get().wordCountInput).getParent();

		WordCounter2 wC = new WordCounter2(
				filePathCut + "words.txt", filePathCut
				+ "wordsdistribution.txt", Config.get().wordCountStats);
		System.out.println(Config.get().wordCountInput + ": ");
		for(int i=65;i<91;i++){
			System.out.println("start counting "+(char)i);
			wC.countWords(i,Config.get().wordCountInput);
			//			System.out.println("counting done, start sorting");
			//			wC.sortWordMap();
			System.out.println("sorting done, start writing to file");
			//wC.printWordsAndDistribution();
			endTime = System.currentTimeMillis();
			sek = (endTime - startTime) / 1000;
			System.out.println("time in sek: "+sek);
		}
		for(int i=97;i<123;i++){
			System.out.println("start counting "+(char)i);
			wC.countWords(i,Config.get().wordCountInput);
			//			System.out.println("counting done, start sorting");
			//			wC.sortWordMap();
			System.out.println("sorting done, start writing to file");
			//wC.printWordsAndDistribution();
		}
		//count remaining signs
		wC.countWords(Config.get().wordCountInput);
		endTime = System.currentTimeMillis();
		sek = (endTime - startTime) / 1000;
		wC.printStats(new File(Config.get().wordCountInput), sek);
		System.out.println("done");

	}

	public WordCounter2(String wordsOutput,
			String wordsDistributionOutput, String statsOutput)
					throws IOException {
		this.wordCount=0;
		this.uniqueWords=0;
		this.wordsWriter = IOHelper.openWriteFile(wordsOutput);
		this.wordsDistributionWriter = IOHelper
				.openWriteFile(wordsDistributionOutput);
		this.statsWriter = new BufferedWriter(new FileWriter(statsOutput, true));
	}

	private void printStats(File file, long sek) throws IOException {

		this.statsWriter.write(file.getAbsolutePath() + ":" + "\n");
		this.statsWriter.write("\t" + "total words: " + this.wordCount);
		if (this.wordCountCheck == this.wordCount) {
			this.statsWriter.write(" (checked)\n");
		} else {
			this.statsWriter.write(" (check failed: should be equal to:"
					+ this.wordCountCheck + ")\n");
		}
		this.statsWriter.write("\t" + "size in bytes: " + file.length() + "\n");
		//	this.statsWriter.write("\t" + "average size of one word in bytes: "
		//	+ file.length() / this.wordCount + "\n");
		this.statsWriter.write("\t" + "unique words: " + this.uniqueWords+ "\n");
		System.out.println("test!");
		this.statsWriter.write("\t" + "seconds to generate stats: " + sek
				+ "\n");
		Date date = new Date();
		this.statsWriter.write("\t" + "date: " + date + "\n");
		this.statsWriter.flush();
		this.statsWriter.close();
	}

	//	private final Comparator<String> StringComparator = new Comparator<String>() {
	//		@Override
	//		public int compare(String a, String b) {
	//			if (WordCounter2.this.wordSet.get(a) >= WordCounter2.this.wordSet
	//					.get(b)) {
	//				return -1;
	//			} else {
	//				return 1;
	//			} // returning 0 would merge keys
	//		}
	//	};

	public void countWords(int asciiCode, String input) throws IOException {
		this.reader = IOHelper.openReadFile(input);
		this.wordSet = new HashSet<String>();
		String[] words;
		while ((this.line = this.reader.readLine()) != null) {
			words = this.line.split("\\s");
			for (String word : words) {
				if(word.startsWith(String.valueOf((char)asciiCode))){
					if (this.wordSet.contains(word)) {
						//	this.wordMap.put(word, this.wordMap.get(word) + 1);
					} else {
						this.wordSet.add(word);
					}
				}
			}
		}
		this.uniqueWords+=this.wordSet.size();
		this.reader.close();
	}

	public void countWords(String input) throws IOException {
		this.reader = IOHelper.openReadFile(input);
		this.wordSet = new HashSet<String>();
		String[] words;
		while ((this.line = this.reader.readLine()) != null) {
			words = this.line.split("\\s");
			for (String word : words) {
				this.wordCount++;
				if(word.length()>0){
					char start=word.toCharArray()[0];
					if(start<65||start>90&&start<97||start>122){
						if (this.wordSet.contains(word)) {
							//this.wordMap.put(word, this.wordMap.get(word) + 1);
						} else {
							this.wordSet.add(word);
						}
					}
				}
			}
		}
		this.uniqueWords+=this.wordSet.size();
		this.reader.close();
	}

	//	public void sortWordMap() {
	//		this.sortedWordMap = new TreeMap<String, Integer>(this.StringComparator);
	//		this.sortedWordMap.putAll(this.wordSet);
	//	}

	//	private void printWordsAndDistribution() throws IOException {
	//		Entry<String, Integer> entry;
	//		int currentOccurrences;
	//		int currentOccurrencesCount;
	//		// first entry has to be handled separately due to initializing
	//		// occurrences
	//		if ((entry = this.sortedWordMap.pollFirstEntry()) != null) {
	//			currentOccurrences = entry.getValue();
	//			currentOccurrencesCount = 1;
	//			this.wordsWriter.write(entry.getKey() + "#" + entry.getValue()
	//					+ "\n");
	//			this.wordsWriter.flush();
	//			this.wordCountCheck += entry.getValue();
	//			// for (int i = 0; i < 1000; i++) {
	//			while (true) {
	//				if ((entry = this.sortedWordMap.pollFirstEntry()) != null) {
	//					// map is not empty
	//					if (currentOccurrences == entry.getValue()) {
	//						currentOccurrencesCount++;
	//					} else {
	//						this.wordsDistributionWriter.write(currentOccurrences
	//								+ "#" + currentOccurrencesCount + "\n");
	//						this.wordsDistributionWriter.flush();
	//						currentOccurrences = entry.getValue();
	//						currentOccurrencesCount = 1;
	//					}
	//					this.wordCountCheck += entry.getValue();
	//					this.wordsWriter.write(entry.getKey() + "#"
	//							+ entry.getValue() + "\n");
	//					this.wordsWriter.flush();
	//				} else {
	//					// map is empty
	//					this.wordsDistributionWriter.write(currentOccurrences + "#"
	//							+ currentOccurrencesCount + "\n");
	//					this.wordsDistributionWriter.flush();
	//					break;
	//				}
	//			}
	//		}
	//		this.wordsWriter.close();
	//		this.wordsDistributionWriter.close();
	//	}
}
