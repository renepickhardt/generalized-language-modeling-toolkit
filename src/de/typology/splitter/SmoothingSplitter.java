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
import de.typology.utils.PatternTransformer;

public class SmoothingSplitter {
	private File inputDirectory;
	private File indexFile;
	private File outputDirectory;
	private String delimiter;
	protected boolean deleteTempFiles;

	static Logger logger = LogManager.getLogger(SmoothingSplitter.class
			.getName());

	public SmoothingSplitter(File inputDirectory, File indexFile,
			File outputDirectory, int maxCountDivider, String delimiter,
			boolean deleteTempFiles) {
		this.inputDirectory = inputDirectory;
		this.indexFile = indexFile;
		this.outputDirectory = outputDirectory;
		this.delimiter = delimiter;
		this.deleteTempFiles = deleteTempFiles;
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

		logger.info("read word index: " + this.indexFile.getAbsolutePath());
		WordIndex wordIndex = new WordIndex(this.indexFile);

		// initialize executerService
		// TODO: change the way, the number of threads (4) is handled
		int cores = 4;
		// int cores = Runtime.getRuntime().availableProcessors();
		ExecutorService executorService = Executors.newFixedThreadPool(cores);
		// for (boolean[] pattern : patterns) {
		boolean[] pattern = patterns.get(6);

		boolean[] newPattern = Arrays.copyOfRange(pattern, 1, pattern.length);
		boolean[] patternForModifying = pattern.clone();
		patternForModifying[0] = false;

		PipedInputStream pipedInputStream = new PipedInputStream(8 * 1024);

		SplitterTask splitterTask = new SplitterTask(pipedInputStream,
				this.outputDirectory, wordIndex, newPattern, this.delimiter, 0,
				this.deleteTempFiles);
		executorService.execute(splitterTask);
		File currentInputDirectory = new File(
				this.inputDirectory.getAbsolutePath() + "/"
						+ PatternTransformer.getStringPattern(pattern));

		try {
			OutputStream pipedOutputStream = new PipedOutputStream(
					pipedInputStream);
			System.out.println(currentInputDirectory.getAbsolutePath());
			SequenceModifier sequenceModifier = new SequenceModifier(
					currentInputDirectory, pipedOutputStream, this.delimiter,
					patternForModifying);
			executorService.execute(sequenceModifier);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// }
		executorService.shutdown();
		try {
			executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void splitGLM(int maxModelLength) {
		ArrayList<boolean[]> patterns = new ArrayList<boolean[]>();
		for (int intPattern = 1; intPattern < Math.pow(2, maxModelLength); intPattern++) {
			// leave out even sequences since they don't contain a
			// target
			if (intPattern % 2 == 0) {
				continue;
			}
			patterns.add(PatternTransformer.getBooleanPattern(intPattern));
		}
		this.split(patterns);
	}

	public void splitGLMForSmoothing(int maxModelLength) {
		ArrayList<boolean[]> patterns = new ArrayList<boolean[]>();
		for (int intPattern = 1; intPattern < Math.pow(2, maxModelLength); intPattern++) {
			// // leave out even sequences since they don't contain a
			// // target
			// if (intPattern % 2 == 0) {
			// continue;
			// }
			patterns.add(PatternTransformer.getBooleanPattern(intPattern));
		}
		this.split(patterns);
	}

	public void splitLM(int maxModelLength) {
		ArrayList<boolean[]> patterns = new ArrayList<boolean[]>();
		for (int intPattern = 1; intPattern < Math.pow(2, maxModelLength); intPattern++) {
			String stringPattern = Integer.toBinaryString(intPattern);
			if (Integer.bitCount(intPattern) == stringPattern.length()) {
				patterns.add(PatternTransformer.getBooleanPattern(intPattern));
			}
		}
		this.split(patterns);
	}

	public void splitTypology(int maxModelLength) {
		ArrayList<boolean[]> patterns = new ArrayList<boolean[]>();
		for (int intPattern = 1; intPattern < Math.pow(2, maxModelLength); intPattern++) {
			String stringPattern = Integer.toBinaryString(intPattern);
			if (Integer.bitCount(intPattern) <= 2
					&& stringPattern.startsWith("1")
					&& stringPattern.endsWith("1")) {
				patterns.add(PatternTransformer.getBooleanPattern(intPattern));
			}
		}
		this.split(patterns);
	}
}
