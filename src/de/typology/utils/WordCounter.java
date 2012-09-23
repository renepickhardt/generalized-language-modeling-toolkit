package de.typology.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;

public class WordCounter {
	private BufferedReader reader;
	private BufferedWriter writer;
	private HashMap<String, Integer> wordMap;
	private TreeMap<String, Integer> sortedWordMap;
	private String line;

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		WordCounter wC = new WordCounter(Config.get().wordCountInput,
				Config.get().wordCountOutput);
		System.out.println("start counting");
		wC.countWords();
		System.out.println("counting done, start sorting");
		wC.sortWordMap();
		System.out.println("sorting done, start writing to file");
		wC.printSortedWordMap();

		System.out.println("generate indicator file");
		File done = new File(Config.get().wordCountOutput + "IsDone");
		done.createNewFile();
		System.out.println("done");
	}

	private final Comparator<String> StringComparator = new Comparator<String>() {
		@Override
		public int compare(String a, String b) {
			if (WordCounter.this.wordMap.get(a) >= WordCounter.this.wordMap
					.get(b)) {
				return -1;
			} else {
				return 1;
			} // returning 0 would merge keys
		}
	};

	public WordCounter(String input, String output) throws IOException {
		this.reader = new BufferedReader(new FileReader(input));
		this.writer = new BufferedWriter(new FileWriter(output));
	}

	public void countWords() throws IOException {
		this.wordMap = new HashMap<String, Integer>();

		while ((this.line = this.reader.readLine()) != null) {
			String[] words = this.line.split(" ");
			for (String word : words) {
				if (this.wordMap.containsKey(word)) {
					this.wordMap.put(word, this.wordMap.get(word) + 1);
				} else {
					this.wordMap.put(word, 1);
				}
			}
		}
		this.reader.close();
	}

	public void sortWordMap() {
		this.sortedWordMap = new TreeMap<String, Integer>(this.StringComparator);
		this.sortedWordMap.putAll(this.wordMap);
	}

	private void printSortedWordMap() throws IOException {
		Entry<String, Integer> entry;
		for (int i = 0; i < 1000; i++) {
			if ((entry = this.sortedWordMap.pollFirstEntry()) != null) {
				this.writer.write(entry.getKey() + "\t" + entry.getValue()
						+ "\n");
			}
			this.writer.flush();
		}
		this.writer.close();
	}
}
