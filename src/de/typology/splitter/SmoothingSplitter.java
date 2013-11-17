package de.typology.splitter;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.typology.indexes.WordIndex;
import de.typology.patterns.PatternTransformer;

public class SmoothingSplitter {
	private File inputDirectory;
	private File _outputDirectory;
	private File _output_Directory;
	private File output_Directory;
	private File indexFile;
	private String delimiter;
	protected boolean deleteTempFiles;

	static Logger logger = LogManager.getLogger(SmoothingSplitter.class
			.getName());
	private ExecutorService executorService;

	public SmoothingSplitter(File inputDirectory, File indexFile,
			int maxCountDivider, String delimiter, boolean deleteTempFiles) {
		this.inputDirectory = inputDirectory;
		this.indexFile = indexFile;
		this.delimiter = delimiter;
		this.deleteTempFiles = deleteTempFiles;
		// delete old directory
		this._outputDirectory = new File(this.inputDirectory.getParent() + "/_"
				+ this.inputDirectory.getName());
		this._output_Directory = new File(this.inputDirectory.getParent()
				+ "/_" + this.inputDirectory.getName() + "_");
		this.output_Directory = new File(this.inputDirectory.getParent() + "/"
				+ this.inputDirectory.getName() + "_");

		if (this._outputDirectory.exists()) {
			try {
				FileUtils.deleteDirectory(this._outputDirectory);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		this._outputDirectory.mkdir();
		if (this.output_Directory.exists()) {
			try {
				FileUtils.deleteDirectory(this.output_Directory);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		this.output_Directory.mkdir();
		if (this._output_Directory.exists()) {
			try {
				FileUtils.deleteDirectory(this._output_Directory);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		this._output_Directory.mkdir();
	}

	public void split(ArrayList<boolean[]> patterns, int cores) {
		// read Index
		logger.info("read word index: " + this.indexFile.getAbsolutePath());
		WordIndex wordIndex = new WordIndex(this.indexFile);
		// initialize executerService
		// int cores = Runtime.getRuntime().availableProcessors();
		this.executorService = Executors.newFixedThreadPool(cores);

		// _absolute
		for (boolean[] inputPattern : patterns) {
			// boolean[] pattern = patterns.get(14);

			String inputPatternLabel = PatternTransformer
					.getStringPattern(inputPattern);
			File currentInputDirectory = new File(this.inputDirectory + "/"
					+ inputPatternLabel);

			int inputPatternNumberOfColumns = Integer
					.bitCount(PatternTransformer.getIntPattern(inputPattern));

			boolean[] inputPatternWithoutFirst = Arrays.copyOfRange(
					inputPattern, 1, inputPattern.length);

			boolean[] newPattern = PatternTransformer
					.getBooleanPatternWithOnes(Integer
							.bitCount(PatternTransformer
									.getIntPattern(inputPatternWithoutFirst)));
			String newPatternLabel = "_"
					+ PatternTransformer
							.getStringPattern(inputPatternWithoutFirst);

			boolean[] patternForModifier = PatternTransformer
					.getBooleanPatternWithOnes(inputPatternNumberOfColumns);
			patternForModifier[0] = false;

			logger.debug("inputPattern: "
					+ PatternTransformer.getStringPattern(inputPattern));
			logger.debug("inputPatternLabel: " + inputPatternLabel);
			logger.debug("newPattern: "
					+ PatternTransformer.getStringPattern(newPattern));
			logger.debug("newPatternLabel: " + newPatternLabel);
			logger.debug("patternForModifier: "
					+ PatternTransformer.getStringPattern(patternForModifier));

			this.splitType(currentInputDirectory, this._outputDirectory,
					newPattern, newPatternLabel, patternForModifier, wordIndex);

		}

		// restart executerService to make sure that all threads are done for
		// building _absolute_
		this.executorService.shutdown();
		try {
			this.executorService.awaitTermination(Long.MAX_VALUE,
					TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		this.executorService = Executors.newFixedThreadPool(cores);

		// _absolute_
		for (boolean[] pattern : patterns) {
			// boolean[] pattern = patterns.get(14);

			// skip patterns that end with a zero
			if (pattern[pattern.length - 1] == false) {
				continue;
			}

			boolean[] inputPattern = Arrays.copyOfRange(pattern, 1,
					pattern.length);
			String inputPatternLabel = "_"
					+ PatternTransformer.getStringPattern(inputPattern);
			File currentInputDirectory = new File(this._outputDirectory + "/"
					+ inputPatternLabel);

			// skip "_"
			if (inputPatternLabel.length() < 2) {
				continue;
			}

			int inputPatternNumberOfColumns = Integer
					.bitCount(PatternTransformer.getIntPattern(inputPattern));

			boolean[] inputPatternWithoutLast = Arrays.copyOfRange(
					inputPattern, 0, inputPattern.length - 1);

			boolean[] newPattern = PatternTransformer
					.getBooleanPatternWithOnes(Integer
							.bitCount(PatternTransformer
									.getIntPattern(inputPatternWithoutLast)));
			String newPatternLabel = "_"
					+ PatternTransformer
							.getStringPattern(inputPatternWithoutLast) + "_";

			boolean[] patternForModifier = PatternTransformer
					.getBooleanPatternWithOnes(inputPatternNumberOfColumns);
			patternForModifier[patternForModifier.length - 1] = false;

			logger.debug("inputPattern: "
					+ PatternTransformer.getStringPattern(inputPattern));
			logger.debug("inputPatternLabel: " + inputPatternLabel);
			logger.debug("newPattern: "
					+ PatternTransformer.getStringPattern(newPattern));
			logger.debug("newPatternLabel: " + newPatternLabel);
			logger.debug("patternForModifier: "
					+ PatternTransformer.getStringPattern(patternForModifier));

			this.splitType(currentInputDirectory, this._output_Directory,
					newPattern, newPatternLabel, patternForModifier, wordIndex);

		}

		// no need to restart executerService since _absolute_ and absolute_ are
		// independent

		// absolute_
		for (boolean[] inputPattern : patterns) {
			// boolean[] pattern = patterns.get(14);

			// skip patterns that end with a zero
			if (inputPattern[inputPattern.length - 1] == false) {
				continue;
			}

			String inputPatternLabel = PatternTransformer
					.getStringPattern(inputPattern);
			File currentInputDirectory = new File(this.inputDirectory + "/"
					+ inputPatternLabel);

			int inputPatternNumberOfColumns = Integer
					.bitCount(PatternTransformer.getIntPattern(inputPattern));

			boolean[] inputPatternWithoutLast = Arrays.copyOfRange(
					inputPattern, 0, inputPattern.length - 1);

			boolean[] newPattern = PatternTransformer
					.getBooleanPatternWithOnes(Integer
							.bitCount(PatternTransformer
									.getIntPattern(inputPatternWithoutLast)));
			String newPatternLabel = PatternTransformer
					.getStringPattern(inputPatternWithoutLast) + "_";

			boolean[] patternForModifier = PatternTransformer
					.getBooleanPatternWithOnes(inputPatternNumberOfColumns);
			patternForModifier[patternForModifier.length - 1] = false;

			logger.debug("inputPattern: "
					+ PatternTransformer.getStringPattern(inputPattern));
			logger.debug("inputPatternLabel: " + inputPatternLabel);
			logger.debug("newPattern: "
					+ PatternTransformer.getStringPattern(newPattern));
			logger.debug("newPatternLabel: " + newPatternLabel);
			logger.debug("patternForModifier: "
					+ PatternTransformer.getStringPattern(patternForModifier));

			this.splitType(currentInputDirectory, this.output_Directory,
					newPattern, newPatternLabel, patternForModifier, wordIndex);

		}

		this.executorService.shutdown();
		try {
			this.executorService.awaitTermination(Long.MAX_VALUE,
					TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void splitType(File currentInputDirectory, File outputDirectory,
			boolean[] newPattern, String newPatternLabel,
			boolean[] patternForModifier, WordIndex wordIndex) {

		PipedInputStream pipedInputStream = new PipedInputStream(8 * 1024);

		if (Integer.bitCount(PatternTransformer.getIntPattern(newPattern)) == 0) {
			LineCounterTask lineCountTask = new LineCounterTask(
					pipedInputStream, outputDirectory, newPatternLabel,
					this.delimiter);
			this.executorService.execute(lineCountTask);
		} else {
			// don't add tags here
			SplitterTask splitterTask = new SplitterTask(pipedInputStream,
					outputDirectory, wordIndex, newPattern, newPatternLabel,
					this.delimiter, 0, this.deleteTempFiles, "", "", true);
			this.executorService.execute(splitterTask);
		}

		try {
			OutputStream pipedOutputStream = new PipedOutputStream(
					pipedInputStream);
			System.out.println(currentInputDirectory.getAbsolutePath());
			SequenceModifier sequenceModifier = new SequenceModifier(
					currentInputDirectory, pipedOutputStream, this.delimiter,
					patternForModifier);
			this.executorService.execute(sequenceModifier);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
