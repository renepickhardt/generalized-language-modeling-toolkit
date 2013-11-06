package de.typology.splitter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;

import de.typology.indexes.WordIndex;
import de.typology.utils.PatternTransformer;

/**
 * A class for running Sequencer and Aggregator for a given pattern.
 * 
 * @author Martin Koerner
 * 
 */
public class SplitterTask implements Runnable {
	protected InputStream inputStream;
	protected File outputDirectory;
	protected WordIndex wordIndex;
	protected boolean[] pattern;
	protected String delimiter;
	protected boolean deleteTempFiles;
	int startSortAtColumn;

	public SplitterTask(InputStream inputStream, File outputDirectory,
			WordIndex wordIndex, boolean[] pattern, String delimiter,
			int startSortAtColumn, boolean deleteTempFiles) {
		this.inputStream = inputStream;
		this.outputDirectory = outputDirectory;
		this.wordIndex = wordIndex;
		this.pattern = pattern;
		this.delimiter = delimiter;
		this.deleteTempFiles = deleteTempFiles;
	}

	@Override
	public void run() {
		File sequencerOutputDirectory = new File(
				this.outputDirectory.getAbsolutePath() + "/"
						+ PatternTransformer.getStringPattern(this.pattern)
						+ "-split");
		if (sequencerOutputDirectory.exists()) {
			try {
				FileUtils.deleteDirectory(sequencerOutputDirectory);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		sequencerOutputDirectory.mkdir();

		// initialize sequencer
		Sequencer sequencer = new Sequencer(this.inputStream,
				sequencerOutputDirectory, this.wordIndex, this.pattern);
		// TODO change method name
		sequencer.run();

		File aggregatedOutputDirectory = new File(
				this.outputDirectory.getAbsolutePath() + "/"
						+ PatternTransformer.getStringPattern(this.pattern));
		if (aggregatedOutputDirectory.exists()) {
			try {
				FileUtils.deleteDirectory(aggregatedOutputDirectory);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		aggregatedOutputDirectory.mkdir();

		for (File splitFile : sequencerOutputDirectory.listFiles()) {
			Aggregator aggregator = new Aggregator(splitFile, new File(
					aggregatedOutputDirectory.getAbsolutePath() + "/"
							+ splitFile.getName()), this.delimiter, 0);
			// TODO change method name
			aggregator.run();
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
