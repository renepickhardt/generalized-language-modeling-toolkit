package de.typology.splitter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
	private File outputDirectory;
	private int maxCountDivider;
	private char delimiter;

	static Logger logger = LogManager.getLogger(Splitter.class.getName());

	public Splitter(File inputFile, File outputDirectory, int maxCountDivider,
			char delimiter) {
		this.inputFile = inputFile;
		this.outputDirectory = outputDirectory;
		this.maxCountDivider = maxCountDivider;
		this.delimiter = delimiter;
	}

	protected void split(ArrayList<boolean[]> patterns) {
		// initialize executerService
		int cores = Runtime.getRuntime().availableProcessors();
		ExecutorService executorService = Executors.newFixedThreadPool(cores);

		// build index file TODO: how to handle index file name?
		logger.trace("build word index");
		WordIndexer wordIndexer = new WordIndexer();
		File indexFile = new File(this.outputDirectory.getAbsolutePath()
				+ "/index.txt");
		wordIndexer.buildIndex(this.inputFile, indexFile, this.maxCountDivider);

		logger.trace("read word index");
		WordIndex wordIndex = new WordIndex(indexFile);
		for (boolean[] pattern : patterns) {
			logger.trace(" split into "
					+ PatternTransformer.getStringPattern(pattern)
					+ " sequences");
			// open inputFile
			InputStream inputFileinputStream;
			try {
				inputFileinputStream = new FileInputStream(this.inputFile);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}
			// initialize sequencer
			Sequencer sequencer = new Sequencer(inputFileinputStream,
					this.outputDirectory, wordIndex, pattern);

			// execute sequencer
			executorService.execute(sequencer);
		}

		executorService.shutdown();
	}
}
