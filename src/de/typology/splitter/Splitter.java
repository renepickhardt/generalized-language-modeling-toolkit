package de.typology.splitter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public abstract class Splitter {
	private String inputPath;
	protected File outputDirectory;
	public HashMap<String, String> wordIndex;
	private BufferedReader reader;

	private Aggregator aggregator;
	private Sorter sorter;
	private CountNormalizer countNormalizer;

	private HashMap<String, BufferedWriter> writers;

	// variables for managing sliding window
	private int linePointer;
	private String line;
	private String[] lineSplit = new String[0];

	protected Splitter(String indexPath, String inputPath,
			String outputDirectoryName) {
		IndexBuilder ib = new IndexBuilder();
		this.wordIndex = ib.deserializeIndex(indexPath);
		this.inputPath = inputPath;
		this.outputDirectory = new File(new File(inputPath).getParent() + "/"
				+ outputDirectoryName);
		this.outputDirectory.mkdir();
		this.aggregator = new Aggregator();
		this.sorter = new Sorter();
		this.countNormalizer = new CountNormalizer();
	}

	protected void initialize(String extension) {
		this.reader = IOHelper.openReadFile(this.inputPath);
		File currentOutputDirectory = new File(
				this.outputDirectory.getAbsoluteFile() + "/" + extension);
		currentOutputDirectory.mkdir();
		this.writers = new HashMap<String, BufferedWriter>();
		for (Entry<String, String> word : this.wordIndex.entrySet()) {
			if (!this.writers.containsKey(word.getValue())) {
				this.writers.put(word.getValue(), IOHelper.openWriteFile(
						currentOutputDirectory + "/" + word.getValue() + "."
								+ extension + "_split",
						Config.get().memoryLimitForWritingFiles
								/ Config.get().maxFiles));
			}
		}
	}

	protected String[] getSequence(int sequenceLength) {
		String[] sequence = new String[sequenceLength];
		if (this.linePointer + sequenceLength > this.lineSplit.length) {
			while (true) {
				// repeat until end of file or finding a line that is long
				// enough
				try {
					this.line = this.reader.readLine();
					if (this.line == null) {
						return null;
					} else {
						this.lineSplit = this.line.split("\\s");
						if (this.lineSplit.length >= sequenceLength) {
							this.linePointer = 0;
							break;
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		for (int i = 0; i < sequenceLength; i++) {
			sequence[i] = this.lineSplit[this.linePointer + i];
		}
		this.linePointer++;
		return sequence;
	}

	protected void sortAndAggregate(String inputPath) {
		this.sorter.sortSplitDirectory(inputPath, "_split", "_splitSort");
		this.aggregator.aggregateDirectory(inputPath, "_splitSort",
				"_aggregate");
		this.sorter.sortCountDirectory(inputPath, "_aggregate", "_countSort");
		this.countNormalizer.normalizeDirectory(inputPath, "_countSort", "");
	}

	protected void reset() {
		this.reader = IOHelper.openReadFile(this.inputPath);
		for (Entry<String, BufferedWriter> writer : this.writers.entrySet()) {
			try {
				writer.getValue().close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	protected BufferedWriter getWriter(String key) {
		return this.writers.get(this.wordIndex.get(key));
	}

	protected abstract void split(int maxSequenceLength);

}
