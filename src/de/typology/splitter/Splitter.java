package de.typology.splitter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.typology.indexes.WordIndex;
import de.typology.indexes.WordIndexer;
import de.typology.utils.PatternTransformer;

/**
 * Split
 * 
 * @author Martin Koerner
 * 
 */
public class Splitter {
	private File inputFile;
	private File inputDirectory;
	private File outputDirectory;
	private int maxCountDivider;
	private String delimiter;

	static Logger logger = LogManager.getLogger(Splitter.class.getName());

	public Splitter(File inputFile, File inputDirectory, File outputDirectory,
			int maxCountDivider, String delimiter) {
		this.inputFile = inputFile;
		this.inputDirectory = inputDirectory;
		this.outputDirectory = outputDirectory;
		this.maxCountDivider = maxCountDivider;
		this.delimiter = delimiter;
		// delete old directory
		if (outputDirectory.exists()) {
			try {
				FileUtils.deleteDirectory(outputDirectory);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		outputDirectory.mkdir();
	}

	protected void split(ArrayList<boolean[]> patterns) {
		// initialize executerService
		int cores = Runtime.getRuntime().availableProcessors();
		ExecutorService executorService = Executors.newFixedThreadPool(cores);

		// build index file TODO: how to handle index file name?
		logger.info("build word index");
		WordIndexer wordIndexer = new WordIndexer();
		File indexFile = new File(this.inputDirectory + "/index.txt");
		System.out.println(indexFile.getAbsolutePath());
		wordIndexer.buildIndex(this.inputFile, indexFile, this.maxCountDivider);

		logger.info("read word index");
		WordIndex wordIndex = new WordIndex(indexFile);

		// copy sequences into different files
		for (boolean[] pattern : patterns) {
			logger.info(" split into "
					+ PatternTransformer.getStringPattern(pattern)
					+ " sequences");
			// open inputFile
			InputStream inputFileinputStream;
			try {
				inputFileinputStream = new FileInputStream(this.inputFile);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				logger.error("inputFile not found: "
						+ this.inputFile.getAbsolutePath());
				return;
			}
			// initialize sequencer
			Sequencer sequencer = new Sequencer(inputFileinputStream,
					this.outputDirectory, wordIndex, pattern);

			// execute sequencer
			executorService.execute(sequencer);
		}

		executorService.shutdown();
		try {
			executorService.awaitTermination(Long.MAX_VALUE,
					TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		executorService = Executors.newFixedThreadPool(cores);
		// aggregate sequences
		for (boolean[] pattern : patterns) {
			File currentSplitDirecotry = new File(
					this.outputDirectory.getAbsolutePath() + "/"
							+ PatternTransformer.getStringPattern(pattern)
							+ "-split");
			File currentAggregatedDirectory = new File(
					this.outputDirectory.getAbsolutePath() + "/"
							+ PatternTransformer.getStringPattern(pattern));
			currentAggregatedDirectory.mkdir();
			for (File splitFile : currentSplitDirecotry.listFiles()) {
				Aggregator aggregator = new Aggregator(splitFile, new File(
						currentAggregatedDirectory.getAbsolutePath() + "/"
								+ splitFile.getName()), this.delimiter, 0);
				executorService.execute(aggregator);

			}
		}

		executorService.shutdown();

	}
}
