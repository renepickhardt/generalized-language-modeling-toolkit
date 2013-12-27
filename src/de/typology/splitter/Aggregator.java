package de.typology.splitter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A class for aggregating sequences by counting their occurrences. Expects an
 * inputStream with a size that is 30% of the allocated main memory.
 * 
 * @author Martin Koerner
 * 
 */
public class Aggregator {
	File inputFile;
	File outputFile;
	String delimiter;
	int startSortAtColumn;
	boolean additionalCounts;

	Logger logger = LogManager.getLogger(this.getClass().getName());

	// this comparator is based on the value of startSortAtColumn
	private Comparator<String> stringComparator = new Comparator<String>() {
		@Override
		public int compare(String string1, String string2) {
			if (Aggregator.this.startSortAtColumn == 0) {
				return string1.compareTo(string2);
			} else {
				// System.out.println(string1);
				// System.out.println(string2);
				String[] string1Split = string1.split("\\s");
				String[] string2Split = string2.split("\\s");
				String newString1 = "";
				String newString2 = "";
				for (int i = Aggregator.this.startSortAtColumn; i < string1Split.length; i++) {
					newString1 += string1Split[i] + " ";
					newString2 += string2Split[i] + " ";
				}
				newString1 = newString1.replaceFirst(" $", "");
				newString2 = newString2.replaceFirst(" $", "");
				// System.out.println(newString1);
				// System.out.println(newString2);
				int result = newString1.compareTo(newString2);
				if (result != 0) {
					// System.out.println("not equal");
					return result;
				} else {
					// System.out.println("equal");
					int i = 0;
					while (i < Aggregator.this.startSortAtColumn) {
						String newNewString1 = newString1;
						String newNewString2 = newString2;
						for (int j = i; j >= 0; j--) {
							newNewString1 = string1Split[j] + " "
									+ newNewString1;
							newNewString2 = string2Split[j] + " "
									+ newNewString2;
						}
						// System.out.println(newNewString1);
						// System.out.println(newNewString2);
						result = newNewString1.compareTo(newNewString2);
						if (result != 0) {
							// System.out.println("not equal");
							return result;
						}
						// System.out.println("equal");
						i++;
					}
					// System.out.println("final result: equal");
					return 0;
				}
			}
		}
	};

	/**
	 * @param inputStream
	 * @param outputStream
	 * @param delimiter
	 * @param startSortAtColumn
	 *            : First column is zero
	 */
	public Aggregator(File inputFile, File outputFile, String delimiter,
			int startSortAtColumn, boolean additionalCounts) {
		this.inputFile = inputFile;
		this.outputFile = outputFile;
		this.delimiter = delimiter;
		this.startSortAtColumn = startSortAtColumn;
		this.additionalCounts = additionalCounts;

	}

	public void aggregateCounts() {
		try {
			BufferedReader inputFileReader = new BufferedReader(new FileReader(
					this.inputFile));

			SortedMap<String, Long[]> wordMapAdditionalCounts = new TreeMap<String, Long[]>(
					this.stringComparator);
			SortedMap<String, Long> wordMapNoAdditionalCounts = new TreeMap<String, Long>(
					this.stringComparator);
			String inputLine;
			// TODO remove
			// System.gc();
			// System.out.println("before reading " + this.inputFile.getName()
			// + ":");
			// this.printMemory();

			while ((inputLine = inputFileReader.readLine()) != null) {
				String[] inputLineSplit = inputLine.split(this.delimiter);
				String words = inputLineSplit[0];
				long count = Long.parseLong(inputLineSplit[1]);
				if (words.length() == 0) {
					// logger.error("empty row in " + this.inputFile + ": \""
					// + inputLine + "\"");
					// logger.error("exiting JVM");
					// System.exit(1);
					continue;
				}

				if (this.additionalCounts) {
					this.addCountWithAdditional(wordMapAdditionalCounts, words,
							count);
				} else {
					this.addCountWithNoAdditional(wordMapNoAdditionalCounts,
							words, count);
				}
			}
			// TODO remove
			// System.out.println("after reading " + this.inputFile.getName()
			// + ":");
			// this.printMemory();
			// System.gc();

			inputFileReader.close();
			BufferedWriter outputFileWriter = new BufferedWriter(
					new FileWriter(this.outputFile));
			if (this.additionalCounts) {
				for (Entry<String, Long[]> entry : wordMapAdditionalCounts
						.entrySet()) {
					String words = entry.getKey();
					// [0]=1+
					// [1]=1
					// [2]=2
					// [3]=3+
					outputFileWriter.write(words + this.delimiter
							+ entry.getValue()[0] + this.delimiter
							+ entry.getValue()[1] + this.delimiter
							+ entry.getValue()[2] + this.delimiter
							+ entry.getValue()[3] + "\n");
				}
			} else {
				for (Entry<String, Long> entry : wordMapNoAdditionalCounts
						.entrySet()) {
					String words = entry.getKey();
					outputFileWriter.write(words + this.delimiter
							+ entry.getValue() + "\n");
				}
			}
			outputFileWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void addCountWithNoAdditional(
			SortedMap<String, Long> wordMapNoAdditionalCounts, String words,
			long count) {
		if (wordMapNoAdditionalCounts.containsKey(words)) {
			wordMapNoAdditionalCounts.put(words,
					wordMapNoAdditionalCounts.get(words) + count);
		} else {
			wordMapNoAdditionalCounts.put(words, count);
		}
	}

	private void addCountWithAdditional(SortedMap<String, Long[]> wordMap,
			String words, long count) {
		if (wordMap.containsKey(words)) {
			Long[] countTypeArray = wordMap.get(words);
			countTypeArray[0] = countTypeArray[0] + count;
			if (count == 1) {
				countTypeArray[1] = countTypeArray[1] + count;
			}
			if (count == 2) {
				countTypeArray[2] = countTypeArray[2] + count;
			}
			if (count >= 3) {
				countTypeArray[3] = countTypeArray[3] + count;
			}
		} else {
			Long[] countTypeArray = new Long[4];
			countTypeArray[0] = count;
			if (count == 1) {
				countTypeArray[1] = count;
			} else {
				countTypeArray[1] = 0L;
			}
			if (count == 2) {
				countTypeArray[2] = count;
			} else {
				countTypeArray[2] = 0L;
			}
			if (count >= 3) {
				countTypeArray[3] = count;
			} else {
				countTypeArray[3] = 0L;
			}
			wordMap.put(words, countTypeArray);
		}
	}

	public void aggregateWithoutCounts() {
		try {
			BufferedReader inputFileReader = new BufferedReader(new FileReader(
					this.inputFile));

			SortedSet<String> wordSet = new TreeSet<String>(
					this.stringComparator);
			String inputLine;
			// TODO remove
			// System.gc();
			// System.out.println("before reading " + this.inputFile.getName()
			// + ":");
			// this.printMemory();

			while ((inputLine = inputFileReader.readLine()) != null) {
				wordSet.add(inputLine);
			}
			inputFileReader.close();
			BufferedWriter outputFileWriter = new BufferedWriter(
					new FileWriter(this.outputFile));
			for (String line : wordSet) {
				outputFileWriter.write(line + "\n");
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
