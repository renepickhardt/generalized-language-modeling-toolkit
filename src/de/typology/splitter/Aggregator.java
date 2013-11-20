package de.typology.splitter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.Comparator;
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
			Comparator<String> stringComparator = new Comparator<String>() {
				@Override
				public int compare(String string1, String string2) {
					if (Aggregator.this.startSortAtColumn == 0) {
						return string1.compareTo(string2);
					} else {
						if (Aggregator.this.startSortAtColumn == 1) {
							int result = string1.substring(
									string1.indexOf(' ') + 1)
									.compareTo(
											string2.substring(string2
													.indexOf(' ') + 1));
							if (result != 0) {
								return result;
							} else {
								return string1.compareTo(string2);
							}
						} else {
							throw new InvalidParameterException(
									"startSortAtColumn greater than 1 is not implemented");
						}
					}
				}
			};
			SortedMap<String, Long> wordMap = new TreeMap<String, Long>(
					stringComparator);
			String inputLine;
			// TODO remove
			System.gc();
			System.out.println("before reading " + this.inputFile.getName()
					+ ":");
			this.printMemory();

			while ((inputLine = inputFileReader.readLine()) != null) {

				String words = inputLine.split(this.delimiter)[0];
				long count = Long.parseLong(inputLine.split(this.delimiter)[1]);
				if (words.length() == 0) {
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
					wordMap.put(words, wordMap.get(words) + count);
				} else {
					// System.out.println();
					// System.out.print("NOT: ");
					// for (String s : words) {
					// System.out.print(s + " ");
					// }
					// System.out.println();
					wordMap.put(words, count);
				}
			}
			// TODO remove
			System.out.println("after reading " + this.inputFile.getName()
					+ ":");
			this.printMemory();
			System.gc();

			inputFileReader.close();
			BufferedWriter outputFileWriter = new BufferedWriter(
					new FileWriter(this.outputFile));
			for (Entry<String, Long> entry : wordMap.entrySet()) {
				String words = entry.getKey();
				outputFileWriter.write(words);

				outputFileWriter
						.write(this.delimiter + entry.getValue() + "\n");
			}
			outputFileWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void printMemory() {

		int mb = 1024 * 1024;

		// Getting the runtime reference from system
		Runtime runtime = Runtime.getRuntime();

		System.out.println("##### Heap utilization statistics [MB] #####");

		// Print used memory
		System.out.println("Used Memory:\t"
				+ (runtime.totalMemory() - runtime.freeMemory()) / mb);

		// Print free memory
		System.out.println("Free Memory:\t" + runtime.freeMemory() / mb);

		// Print total available memory
		System.out.println("Total Memory:\t" + runtime.totalMemory() / mb);

		// Print Maximum available memory
		System.out.println("Max Memory:\t" + runtime.maxMemory() / mb);
		System.out.println("");

	}
}
