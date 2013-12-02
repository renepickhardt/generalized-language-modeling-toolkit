package de.typology.splitter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.typology.indexes.WordIndex;

/**
 * A class for running Sequencer and Aggregator for a given pattern.
 * 
 * @author Martin Koerner
 * 
 */
public class SplitterTask implements Runnable {
	private InputStream inputStream;
	private File outputDirectory;
	private WordIndex wordIndex;
	private boolean[] pattern;
	private String patternLabel;
	private String delimiter;
	private int startSortAtColumn;
	private boolean deleteTempFiles;

	private String addBeforeSentence;
	private String addAfterSentence;
	private boolean completeLine;

	Logger logger = LogManager.getLogger(this.getClass().getName());

	public SplitterTask(InputStream inputStream, File outputDirectory,
			WordIndex wordIndex, boolean[] pattern, String patternLabel,
			String delimiter, int startSortAtColumn, boolean deleteTempFiles,
			String addBeforeSentence, String addAfterSentence,
			boolean completeLine) {
		this.inputStream = inputStream;
		this.outputDirectory = outputDirectory;
		this.wordIndex = wordIndex;
		this.pattern = pattern;
		this.patternLabel = patternLabel;
		this.delimiter = delimiter;
		this.startSortAtColumn = startSortAtColumn;
		this.deleteTempFiles = deleteTempFiles;
		this.addBeforeSentence = addBeforeSentence;
		this.addAfterSentence = addAfterSentence;
		this.completeLine = completeLine;
	}

	@Override
	public void run() {
		File sequencerOutputDirectory = new File(
				this.outputDirectory.getAbsolutePath() + "/"
						+ this.patternLabel + "-split");
		if (sequencerOutputDirectory.exists()) {
			try {
				FileUtils.deleteDirectory(sequencerOutputDirectory);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		sequencerOutputDirectory.mkdir();
		this.logger.info("start building: "
				+ sequencerOutputDirectory.getAbsolutePath());

		// initialize sequencer
		Sequencer sequencer = new Sequencer(this.inputStream,
				sequencerOutputDirectory, this.wordIndex, this.pattern,
				this.addBeforeSentence, this.addAfterSentence, this.delimiter,
				this.completeLine, this.startSortAtColumn);
		sequencer.splitIntoFiles();

		File aggregatedOutputDirectory = new File(
				this.outputDirectory.getAbsolutePath() + "/"
						+ this.patternLabel);
		if (aggregatedOutputDirectory.exists()) {
			try {
				FileUtils.deleteDirectory(aggregatedOutputDirectory);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		aggregatedOutputDirectory.mkdir();
		this.logger.info("aggregate into: " + aggregatedOutputDirectory);

		for (File splitFile : sequencerOutputDirectory.listFiles()) {
			Aggregator aggregator = new Aggregator(splitFile, new File(
					aggregatedOutputDirectory.getAbsolutePath() + "/"
							+ splitFile.getName()), this.delimiter,
					this.startSortAtColumn);
			if (this.completeLine) {
				aggregator.aggregateCounts();
			} else {
				aggregator.aggregateWithoutCounts();
			}
		}

		// delete sequencerOutputDirectory
		if (this.deleteTempFiles) {
			try {
				FileUtils.deleteDirectory(sequencerOutputDirectory);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
