package de.typology.splitter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A class for aggregating sequences by counting their occurrences. Expects an
 * inputStream with a size that is 30% of the allocated main memory.
 * 
 * @author Martin Koerner
 * 
 */
public class Aggregator implements Runnable {
	File inputFile;
	File outputFile;
	String delimiter;
	int startSortAtColumn;

	static Logger logger = LogManager.getLogger(Aggregator.class.getName());

	/**
	 * @param inputStream
	 * @param outputStream
	 * @param delimiter
	 * @param startSortAtColumn
	 *            : First column is zero
	 */
	public Aggregator(File inputFile, File outputFile, String delimiter,
			int startSortAtColumn) {
		this.inputFile = inputFile;
		this.outputFile = outputFile;
		this.delimiter = delimiter;
		this.startSortAtColumn = startSortAtColumn;

	}

	@Override
	public void run() {
		try {
			BufferedReader inputFileReader = new BufferedReader(new FileReader(
					this.inputFile));

			// this comparator is based on the value of startSortAtColumn
			Comparator<List<String>> arrayComparator = new Comparator<List<String>>() {
				@Override
				public int compare(List<String> strings1, List<String> strings2) {
					// start comparison at "startSortAtColumn"
					for (int i = Aggregator.this.startSortAtColumn; i < strings1
							.size(); i++) {
						if (!strings1.get(i).equals(strings2.get(i))) {
							return strings1.get(i).compareTo(strings2.get(i));
						}
					}
					// all columns starting at "startSortAtColumn" are equal so
					// check the columns before "startSortAtColumn"
					for (int i = 0; i < Aggregator.this.startSortAtColumn; i++) {
						if (!strings1.get(i).equals(strings2.get(i))) {
							return strings1.get(i).compareTo(strings2.get(i));
						}
					}
					return 0;
				}
			};
			SortedMap<List<String>, Long> wordMap = new TreeMap<List<String>, Long>(
					arrayComparator);
			String inputLine;
			while ((inputLine = inputFileReader.readLine()) != null) {
				List<String> words = Arrays.asList(inputLine.split("\\s"));
				if (words.size() == 0) {
					// logger.error("empty row in " + this.inputFile + ": \""
					// + inputLine + "\"");
					// logger.error("exiting JVM");
					// System.exit(1);
					continue;
				}
				if (wordMap.containsKey(words)) {
					// System.out.println();
					// System.out.print("IN: ");
					// for (String s : words) {
					// System.out.print(s + " ");
					// }
					// System.out.println();
					wordMap.put(words, wordMap.get(words) + 1L);
				} else {
					// System.out.println();
					// System.out.print("NOT: ");
					// for (String s : words) {
					// System.out.print(s + " ");
					// }
					// System.out.println();
					wordMap.put(words, 1L);
				}
			}
			inputFileReader.close();
			BufferedWriter outputFileWriter = new BufferedWriter(
					new FileWriter(this.outputFile));
			for (Entry<List<String>, Long> entry : wordMap.entrySet()) {
				List<String> words = entry.getKey();
				for (int i = 0; i < words.size() - 1; i++) {
					outputFileWriter.write(words.get(i) + " ");
				}
				outputFileWriter.write(words.get(words.size() - 1));

				outputFileWriter
						.write(this.delimiter + entry.getValue() + "\n");
			}
			outputFileWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
