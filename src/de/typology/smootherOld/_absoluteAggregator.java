package de.typology.smootherOld;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;

import de.typology.splitterOld.BinarySearch;
import de.typology.splitterOld.IndexBuilder;
import de.typology.splitterOld.Sorter;
import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public class _absoluteAggregator {

	/**
	 * @param args
	 */
	// public static void main(String[] args) {
	// // TODO Auto-generated method stub
	// }

	private String directory;
	private BufferedReader reader;
	private File inputDirectory;
	private File outputDirectory;
	private HashMap<Integer, BufferedWriter> writers;
	private String indexPath;
	private String[] wordIndex;
	private Sorter sorter;

	public _absoluteAggregator(String directory, String inputDirectoryName,
			String outputDirectoryName, String indexName) {
		this.directory = directory;
		this.inputDirectory = new File(directory + inputDirectoryName);
		this.outputDirectory = new File(directory + outputDirectoryName);
		// delete old output directory
		try {
			FileUtils.deleteDirectory(this.outputDirectory);
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.outputDirectory.mkdir();
		IndexBuilder ib = new IndexBuilder();
		this.indexPath = this.directory + indexName;
		this.wordIndex = ib.deserializeIndex(this.indexPath);
		this.sorter = new Sorter();
	}

	public void aggregate(int maxSequenceLength) {
		IOHelper.strongLog("aggregating continuation values of "
				+ this.directory + " into " + this.outputDirectory);
		IOHelper.strongLog("DELETE TEMP FILES IS: "
				+ Config.get().deleteTempFiles);

		// leave out unigrams
		for (int sequenceDecimal = 2; sequenceDecimal < Math.pow(2,
				maxSequenceLength); sequenceDecimal++) {
			// leave out even sequences since they don't contain a
			// target
			if (sequenceDecimal % 2 == 0) {
				continue;
			}
			String sequenceBinary = Integer.toBinaryString(sequenceDecimal);
			this.aggregateFiles(sequenceBinary);
			String outputSequenceBinary = sequenceBinary.replaceFirst("1", "_");
			// merge and sort current directory
			this.sorter.sortCountDirectory(
					this.outputDirectory.getAbsolutePath() + "/"
							+ outputSequenceBinary, "-split", "");
		}

		// // count absolute files
		// GLMCounter glmc = new GLMCounter(this.directory, "absolute",
		// "counts-absolute");
		// glmc.countAbsolute(1);
		// glmc = new GLMCounter(this.directory, "aggregate",
		// "counts-aggregate");
		// glmc.countAbsolute(2);

		// delete unaggregated continuation directory
		if (Config.get().deleteTempFiles) {
			try {
				FileUtils.deleteDirectory(this.inputDirectory);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	private void aggregateFiles(String sequenceBinary) {
		IOHelper.strongLog("aggregate " + sequenceBinary);
		String outputSequenceBinary = sequenceBinary.replaceFirst("1", "_");
		File currentInputDirectory = new File(this.inputDirectory + "/"
				+ sequenceBinary);
		File currentOutputDirectory = new File(this.outputDirectory + "/"
				+ outputSequenceBinary);
		currentOutputDirectory.mkdir();
		this.initialize(sequenceBinary);

		for (File currentFile : currentInputDirectory.listFiles()) {
			this.reader = IOHelper.openReadFile(currentFile.getAbsolutePath(),
					Config.get().memoryLimitForReadingFiles);
			String line;
			String lineSplit[] = null;
			String currentSequence = null;
			long currentCount = 0;
			try {
				while ((line = this.reader.readLine()) != null) {
					lineSplit = line.split("\t");
					String tempSequence = "";
					for (int i = 1; i < lineSplit.length - 1; i++) {
						tempSequence += lineSplit[i] + "\t";
					}
					if (lineSplit.length == 3 && lineSplit[1].equals("<s>")) {
						this.getWriter("<s>").write(tempSequence + "0\n");
						continue;
					}
					if (currentSequence == null) {
						// <fs> always counts as a different start
						if (line.startsWith("<fs>\t")) {
							currentSequence = tempSequence;
							currentCount = Long
									.parseLong(lineSplit[lineSplit.length - 1]);
						} else {
							// initialize
							currentSequence = tempSequence;
							currentCount = 1;
						}
					} else {
						if (tempSequence.equals(currentSequence)) {
							currentCount++;
						} else {
							this.getWriter(currentSequence.split("\t")[0])
									.write(currentSequence + currentCount
											+ "\n");
							// <fs> always counts as a different start
							if (line.startsWith("<fs>\t")) {
								currentSequence = tempSequence;
								currentCount = Long
										.parseLong(lineSplit[lineSplit.length - 1]);
							} else {
								currentSequence = tempSequence;
								currentCount = 1;
							}
						}
					}
				}
				if (currentSequence != null) {
					this.getWriter(currentSequence.split("\t")[0]).write(
							currentSequence + currentCount + "\n");
				}
				this.reader.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		this.reset();
	}

	protected void initialize(String extension) {
		String outputExtension = extension.replaceFirst("1", "_");
		File currentOutputDirectory = new File(
				this.outputDirectory.getAbsoluteFile() + "/" + outputExtension);
		if (currentOutputDirectory.exists()) {
			try {
				FileUtils.deleteDirectory(currentOutputDirectory);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		currentOutputDirectory.mkdir();
		this.writers = new HashMap<Integer, BufferedWriter>();
		for (int fileCount = 0; fileCount < this.wordIndex.length; fileCount++) {
			this.writers.put(fileCount, IOHelper.openWriteFile(
					currentOutputDirectory + "/" + fileCount + "."
							+ outputExtension + "-split",
					Config.get().memoryLimitForWritingFiles
							/ Config.get().maxCountDivider));
		}
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
}
