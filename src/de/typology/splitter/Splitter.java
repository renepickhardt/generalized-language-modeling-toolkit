package de.typology.splitter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;

import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public abstract class Splitter {
	private String directory;
	private String inputName;
	private String statsPath;
	private String indexPath;
	protected File outputDirectory;
	private String[] wordIndex;
	protected BufferedReader reader;

	private SecondLevelSplitter secondLevelSplitter;
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
		this.indexPath = directory + indexName;
		IndexBuilder ib = new IndexBuilder();
		if (indexName != null) {
			this.wordIndex = ib.deserializeIndex(this.indexPath);
		}

		this.outputDirectory = new File(this.directory + "/"
				+ outputDirectoryName + "-normalized");
		this.outputDirectory.mkdir();
		this.secondLevelSplitter = new SecondLevelSplitter();
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
									/ Config.get().maxCountDivider));
		}
	}

	/**
	 * This method is used when having ngrams as an input
	 * 
	 * @param extension
	 * @param sequenceLength
	 */
	protected void initializeWithLength(String extension, int sequenceLength) {
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
									/ Config.get().maxCountDivider));
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
		File normalizedNGrams = new File(inputPath);
		File parentDir = normalizedNGrams.getParentFile();
		File absoluteNGramsParent = new File(parentDir.getParentFile()
				.getAbsolutePath()
				+ "/"
				+ parentDir.getName().replace("-normalized", "-absolute"));
		File absoluteNGrams = new File(absoluteNGramsParent.getAbsolutePath()
				+ "/" + normalizedNGrams.getName());
		absoluteNGramsParent.mkdir();
		absoluteNGrams.mkdir();
		this.secondLevelSplitter.secondLevelSplitDirectory(this.indexPath,
				normalizedNGrams.getAbsolutePath(), "_split", "_split");
		this.sorter.sortSplitDirectory(normalizedNGrams.getAbsolutePath(),
				"_split", "_splitSort");
		this.aggregator.aggregateDirectory(normalizedNGrams.getAbsolutePath(),
				"_splitSort", "_aggregate");
		this.sorter.sortCountDirectory(normalizedNGrams.getAbsolutePath(),
				"_aggregate", "_countSort");
		try {
			FileUtils.copyDirectory(normalizedNGrams, absoluteNGrams);
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.countNormalizer.normalizeDirectory(this.statsPath,
				normalizedNGrams.getAbsolutePath(), "_countSort", "");

		this.secondLevelSplitter.mergeDirectory(normalizedNGrams
				.getAbsolutePath());
		this.mergeSmallestType(normalizedNGrams.getAbsolutePath());

		// rename absoulte ngram files
		for (File file : absoluteNGrams.listFiles()) {
			file.renameTo(new File(file.getAbsolutePath().replace("_countSort",
					"")));
		}
		this.secondLevelSplitter.mergeDirectory(absoluteNGrams
				.getAbsolutePath());
		this.mergeSmallestType(absoluteNGrams.getAbsolutePath());
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

	protected abstract void mergeSmallestType(String inputPath);

	public void initializeForSequenceSplit(String fileName) {
		this.reader = IOHelper.openReadFile(this.directory + fileName);
	}

}
