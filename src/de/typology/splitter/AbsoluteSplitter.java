package de.typology.splitter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.typology.indexes.WordIndex;
import de.typology.patterns.PatternTransformer;

/**
 * Split
 * 
 * @author Martin Koerner
 * 
 */
public class AbsoluteSplitter {
	private File inputFile;
	private File indexFile;
	private File outputDirectory;
	private String delimiter;
	protected boolean deleteTempFiles;
	protected String addBeforeSentence;
	protected String addAfterSentence;

	static Logger logger = LogManager.getLogger(AbsoluteSplitter.class
			.getName());

	public AbsoluteSplitter(File inputFile, File indexFile,
			File outputDirectory, int maxCountDivider, String delimiter,
			boolean deleteTempFiles, String addBeforeSentence,
			String addAfterSentence) {
		this.inputFile = inputFile;
		this.indexFile = indexFile;
		this.outputDirectory = outputDirectory;
		this.delimiter = delimiter;
		this.deleteTempFiles = deleteTempFiles;
		this.addBeforeSentence = addBeforeSentence;
		this.addAfterSentence = addAfterSentence;
		// delete old directory
		// TODO uncomment
		// if (outputDirectory.exists()) {
		// try {
		// FileUtils.deleteDirectory(outputDirectory);
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// }
		// outputDirectory.mkdir();
	}

	public void split(ArrayList<boolean[]> patterns, int cores) {

		logger.info("read word index: " + this.indexFile.getAbsolutePath());
		WordIndex wordIndex = new WordIndex(this.indexFile);

		// initialize executerService
		// int cores = Runtime.getRuntime().availableProcessors();
		ExecutorService executorService = Executors.newFixedThreadPool(cores);
		for (boolean[] pattern : patterns) {
			logger.debug("execute SplitterTask for: "
					+ PatternTransformer.getStringPattern(pattern)
					+ " sequences");

			try {
				InputStream inputFileInputStream = new FileInputStream(
						this.inputFile);
				SplitterTask splitterTask = new SplitterTask(
						inputFileInputStream, this.outputDirectory, wordIndex,
						pattern, PatternTransformer.getStringPattern(pattern),
						this.delimiter, 0, this.deleteTempFiles,
						this.addBeforeSentence, this.addAfterSentence, false);
				executorService.execute(splitterTask);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				logger.error("inputFile not found: "
						+ this.inputFile.getAbsolutePath());
				return;
			}
		}
		executorService.shutdown();
		try {
			executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
