package de.typology.splitter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

import de.typology.utils.BinarySearch;
import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public abstract class Splitter {
	private String directory;
	private String inputName;
	private String statsPath;
	protected File outputDirectory;
	private String[] wordIndex;
	protected BufferedReader reader;

	private Aggregator aggregator;
	private Sorter sorter;
	private CountNormalizer countNormalizer;

	private HashMap<Integer, BufferedWriter> writers;

	// variables for managing sliding window
	private int linePointer;
	private String line;
	private String[] lineSplit = new String[0];

	// sequence and sequenceCount are used by split()
	protected String[] sequence;
	protected int sequenceCount;

	protected Splitter(String directory, String indexName, String statsName,
			String inputName, String outputDirectoryName) {
		this.directory = directory;
		this.inputName = inputName;
		this.statsPath = directory + statsName;
		IndexBuilder ib = new IndexBuilder();
		this.wordIndex = ib.deserializeIndex(directory + indexName);

		this.outputDirectory = new File(this.directory + "/"
				+ outputDirectoryName);
		this.outputDirectory.mkdir();
		this.aggregator = new Aggregator();
		this.sorter = new Sorter();
		this.countNormalizer = new CountNormalizer();
	}

	/**
	 * Initializing the reader and writers
	 * 
	 * int sequenceLength is not used but necessary for overriding the method
	 * later with initializingWithLength()
	 * 
	 * @param extension
	 * @param sequenceLength
	 */
	protected void initialize(String extension, int sequenceLength) {
		this.reader = IOHelper.openReadFile(this.directory + this.inputName);
		File currentOutputDirectory = new File(
				this.outputDirectory.getAbsoluteFile() + "/" + extension);

		// delete old files
		IOHelper.deleteDirectory(currentOutputDirectory);

		currentOutputDirectory.mkdir();
		this.writers = new HashMap<Integer, BufferedWriter>();
		for (int fileCount = 0; fileCount < this.wordIndex.length; fileCount++) {
			this.writers.put(
					fileCount,
					IOHelper.openWriteFile(
							currentOutputDirectory + "/" + fileCount + "."
									+ extension + "_split",
							Config.get().memoryLimitForWritingFiles
									/ Config.get().maxFiles));
		}
	}

	/**
	 * This method is used when having ngrams as an input
	 * 
	 * @param extension
	 * @param sequenceLength
	 */
	protected void initializeWithLength(String extension, int sequenceLength) {
		this.reader = IOHelper.openReadFile(this.directory + sequenceLength
				+ "/" + this.inputName);
		File currentOutputDirectory = new File(
				this.outputDirectory.getAbsoluteFile() + "/" + extension);

		// delete old files
		IOHelper.deleteDirectory(currentOutputDirectory);

		currentOutputDirectory.mkdir();
		this.writers = new HashMap<Integer, BufferedWriter>();
		for (int fileCount = 0; fileCount < this.wordIndex.length; fileCount++) {
			this.writers.put(
					fileCount,
					IOHelper.openWriteFile(
							currentOutputDirectory + "/" + fileCount + "."
									+ extension + "_split",
							Config.get().memoryLimitForWritingFiles
									/ Config.get().maxFiles));
		}
	}

	/**
	 * this method assumes that there is no count at the end of a line
	 * 
	 * @param sequenceLength
	 * @return
	 */
	protected boolean getNextSequence(int sequenceLength) {
		this.sequence = new String[sequenceLength];
		if (this.linePointer + sequenceLength > this.lineSplit.length) {
			while (true) {
				// repeat until end of file or finding a line that is long
				// enough
				try {
					this.line = this.reader.readLine();
					if (this.line == null) {
						// reached end of file
						return false;
					} else {
						this.lineSplit = this.line.split("\\s+");
						if (this.lineSplit.length >= sequenceLength) {
							this.linePointer = 0;
							this.sequenceCount = 1;
							break;
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		for (int i = 0; i < sequenceLength; i++) {
			this.sequence[i] = this.lineSplit[this.linePointer + i];
		}
		this.linePointer++;
		return true;
	}

	/**
	 * this method assumes that there is a count at the end of a line
	 * 
	 * @param sequenceLength
	 * @return
	 */
	protected boolean getNextSequenceWithCount(int sequenceLength) {
		this.sequence = new String[sequenceLength];
		// this.lineSplit.length-1 to leave out the count
		if (this.linePointer + sequenceLength > this.lineSplit.length - 1) {
			while (true) {
				// repeat until end of file or finding a line that is long
				// enough
				try {
					this.line = this.reader.readLine();
					if (this.line == null) {
						// reached end of file
						return false;
					} else {
						this.lineSplit = this.line.split("\\s");
						if (this.lineSplit.length > sequenceLength) {
							this.linePointer = 0;
							this.sequenceCount = Integer
									.parseInt(this.lineSplit[this.lineSplit.length - 1]);
							break;
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		for (int i = 0; i < sequenceLength; i++) {
			this.sequence[i] = this.lineSplit[this.linePointer + i];
		}
		this.linePointer++;
		return true;
	}

	protected void sortAndAggregate(String inputPath) {
		this.sorter.sortSplitDirectory(inputPath, "_split", "_splitSort");
		this.aggregator.aggregateDirectory(inputPath, "_splitSort",
				"_aggregate");
		this.sorter.sortCountDirectory(inputPath, "_aggregate", "_countSort");
		this.countNormalizer.normalizeDirectory(this.statsPath, inputPath,
				"_countSort", "");
	}

	protected void reset() {
		for (Entry<Integer, BufferedWriter> writer : this.writers.entrySet()) {
			try {
				writer.getValue().close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	protected BufferedWriter getWriter(String key) {
		return this.writers.get(BinarySearch.rank(key, this.wordIndex));

	}

	protected abstract void split(int maxSequenceLength);

}
