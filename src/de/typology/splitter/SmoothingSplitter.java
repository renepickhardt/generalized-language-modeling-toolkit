package de.typology.splitter;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.typology.indexes.WordIndex;
import de.typology.patterns.PatternTransformer;

public class SmoothingSplitter {
	private File inputDirectory;
	private File indexFile;
	private String delimiter;
	protected boolean deleteTempFiles;

	Logger logger = LogManager.getLogger(this.getClass().getName());
	private ExecutorService executorService;

	private Comparator<boolean[]> patternComparator = new Comparator<boolean[]>() {
		@Override
		public int compare(boolean[] pattern1, boolean[] pattern2) {
			return PatternTransformer.getStringPattern(pattern2).compareTo(
					PatternTransformer.getStringPattern(pattern1));
		}
	};

	public SmoothingSplitter(File inputDirectory, File indexFile,
			int maxCountDivider, String delimiter, boolean deleteTempFiles) {
		this.inputDirectory = inputDirectory;
		this.indexFile = indexFile;
		this.delimiter = delimiter;
		this.deleteTempFiles = deleteTempFiles;
	}

	public void split(ArrayList<boolean[]> patterns, int cores) {
		// read Index
		this.logger
				.info("read word index: " + this.indexFile.getAbsolutePath());
		WordIndex wordIndex = new WordIndex(this.indexFile);
		// initialize executerService
		// int cores = Runtime.getRuntime().availableProcessors();
		this.executorService = Executors.newFixedThreadPool(cores);

		SortedMap<boolean[], boolean[]> continuationMap = this
				.filterContinuationMap(this.getContinuationMap(patterns));

		for (Entry<boolean[], boolean[]> entry : continuationMap.entrySet()) {
			System.out.println(PatternTransformer.getStringPattern(entry
					.getKey())
					+ " <-- "
					+ PatternTransformer.getStringPattern(entry.getValue()));
		}
		System.out.println(continuationMap.size());

		// TODO build continuation sequences
		this.executorService.shutdown();
		try {
			this.executorService.awaitTermination(Long.MAX_VALUE,
					TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private SortedMap<boolean[], boolean[]> filterContinuationMap(
			SortedMap<boolean[], boolean[]> continuationMap) {
		SortedMap<boolean[], boolean[]> newContinuationMap = new TreeMap<boolean[], boolean[]>(
				this.patternComparator);
		for (Entry<boolean[], boolean[]> entry : continuationMap.entrySet()) {
			if (PatternTransformer.getStringPattern(entry.getKey()).equals(
					PatternTransformer.getStringPattern(entry.getValue()))) {
				continue;
			}
			boolean[] currentPattern = entry.getKey();
			if (currentPattern.length > 2) {
				if (!currentPattern[0]
						&& currentPattern[1]
						&& Integer.bitCount(PatternTransformer
								.getIntPattern(currentPattern)) < currentPattern.length - 1) {
					continue;
				}
				if (!currentPattern[0]
						&& !currentPattern[1]
						&& Integer.bitCount(PatternTransformer
								.getIntPattern(currentPattern)) < currentPattern.length - 2) {
					continue;
				}
			}
			newContinuationMap.put(entry.getKey(), entry.getValue());

		}
		return newContinuationMap;
	}

	private SortedMap<boolean[], boolean[]> getContinuationMap(
			ArrayList<boolean[]> patterns) {
		SortedMap<boolean[], boolean[]> continuationMap = new TreeMap<boolean[], boolean[]>(
				this.patternComparator);

		for (boolean[] inputPattern : patterns) {
			this.addPatterns(continuationMap, inputPattern, inputPattern, 0);
		}
		return continuationMap;
	}

	private void addPatterns(SortedMap<boolean[], boolean[]> continuationMap,
			boolean[] pattern, boolean[] oldPattern, int position) {
		if (position < pattern.length) {
			boolean[] newPattern = pattern.clone();
			newPattern[position] = false;
			continuationMap.put(newPattern, pattern);
			continuationMap.put(pattern, oldPattern);
			this.addPatterns(continuationMap, newPattern, pattern, position + 1);
			this.addPatterns(continuationMap, pattern, oldPattern, position + 1);
		}
	}

	private void splitType(File currentInputDirectory, File outputDirectory,
			boolean[] newPattern, String newPatternLabel,
			boolean[] patternForModifier, WordIndex wordIndex,
			boolean setCountToOne) {

		PipedInputStream pipedInputStream = new PipedInputStream(100 * 8 * 1024);

		if (Integer.bitCount(PatternTransformer.getIntPattern(newPattern)) == 0) {
			LineCounterTask lineCountTask = new LineCounterTask(
					pipedInputStream, outputDirectory, newPatternLabel,
					this.delimiter);
			this.executorService.execute(lineCountTask);
		} else {
			// don't add tags here
			SplitterTask splitterTask = new SplitterTask(pipedInputStream,
					outputDirectory, wordIndex, newPattern, newPatternLabel,
					this.delimiter, 0, this.deleteTempFiles, "", "", true,
					false);
			this.executorService.execute(splitterTask);
		}

		try {
			OutputStream pipedOutputStream = new PipedOutputStream(
					pipedInputStream);
			System.out.println(currentInputDirectory.getAbsolutePath());
			SequenceModifier sequenceModifier = new SequenceModifier(
					currentInputDirectory, pipedOutputStream, this.delimiter,
					patternForModifier, true, setCountToOne);
			this.executorService.execute(sequenceModifier);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
