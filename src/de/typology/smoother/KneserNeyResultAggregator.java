package de.typology.smoother;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.typology.indexes.WordIndex;
import de.typology.patterns.PatternTransformer;
import de.typology.splitter.SequenceModifier;
import de.typology.splitter.SplitterTask;
import de.typology.utils.DecimalFormatter;
import de.typology.utils.SlidingWindowReader;

public class KneserNeyResultAggregator {
	private File lowOrderResultDirectory;
	private File tempResultDirectory;

	private WordIndex wordIndex;
	private String delimiter;
	private DecimalFormatter decimalFormatter;
	private boolean deleteTempFiles;

	private Logger logger = LogManager.getLogger(this.getClass().getName());

	public KneserNeyResultAggregator(File lowOrderResultDirectory,
			File tempResultDirectory, WordIndex wordIndex, String delimiter,
			int decimalPlaces, boolean deleteTempFiles) {
		this.lowOrderResultDirectory = lowOrderResultDirectory;
		this.tempResultDirectory = tempResultDirectory;
		this.wordIndex = wordIndex;
		this.delimiter = delimiter;
		this.decimalFormatter = new DecimalFormatter(decimalPlaces);
		this.deleteTempFiles = deleteTempFiles;
	}

	public void aggregate(boolean[] currentPattern,
			ArrayList<boolean[]> backoffPatterns, int cores) {
		ExecutorService executorService;
		int currentTempDirNumber = 1;
		for (boolean[] backoffPattern : backoffPatterns) {
			File currentLowOrderResultDirectory = new File(
					this.lowOrderResultDirectory.getAbsolutePath()
							+ "/"
							+ PatternTransformer
									.getStringPattern(backoffPattern));
			File currentTempResultDirectory = new File(
					this.tempResultDirectory.getAbsolutePath()
							+ "/"
							+ PatternTransformer
									.getStringPattern(currentPattern) + "-"
							+ currentTempDirNumber);
			currentTempResultDirectory.mkdir();

			if (backoffPattern.length == currentPattern.length - 1) {
				// lower order pattern with first word missing
				File currentTempResult2ndDirectory = new File(
						currentTempResultDirectory.getAbsolutePath() + "-2nd");
				currentTempResult2ndDirectory.mkdir();
				File currentTempResult2ndAggregatedDirectory = new File(
						currentTempResult2ndDirectory.getAbsolutePath()
								+ "-aggr");
				currentTempResult2ndAggregatedDirectory.mkdir();

				// sort by second column

				// initialize executerService
				executorService = Executors.newFixedThreadPool(cores);

				PipedInputStream pipedInputStream = new PipedInputStream(
						100 * 8 * 1024);

				// don't add tags here
				SplitterTask splitterTask = new SplitterTask(pipedInputStream,
						this.tempResultDirectory, this.wordIndex,
						currentPattern,
						currentTempResult2ndDirectory.getName(),
						this.delimiter, 1, this.deleteTempFiles, "", "", true);
				executorService.execute(splitterTask);

				try {
					OutputStream pipedOutputStream = new PipedOutputStream(
							pipedInputStream);

					SequenceModifier sequenceModifier = new SequenceModifier(
							new File(this.tempResultDirectory.getAbsolutePath()
									+ "/"
									+ PatternTransformer
											.getStringPattern(currentPattern)),
							pipedOutputStream, this.delimiter, null, false,
							false);
					executorService.execute(sequenceModifier);

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				executorService.shutdown();
				try {
					executorService.awaitTermination(Long.MAX_VALUE,
							TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				for (File currentTempResult2ndFile : currentTempResult2ndDirectory
						.listFiles()) {
					try {
						BufferedReader currentTempResult2ndReader = new BufferedReader(
								new FileReader(currentTempResult2ndFile));

						File currentLowOrderResultFile = new File(
								currentLowOrderResultDirectory
										.getAbsolutePath()
										+ "/"
										+ currentTempResult2ndFile.getName());
						SlidingWindowReader currentLowOrderResultReader = new SlidingWindowReader(
								new FileReader(currentLowOrderResultFile));

						File currentTempResult2ndAggregatedFile = new File(
								currentTempResult2ndAggregatedDirectory
										.getAbsolutePath()
										+ "/"
										+ currentTempResult2ndFile.getName());
						BufferedWriter aggregated2ndResultWriter = new BufferedWriter(
								new FileWriter(
										currentTempResult2ndAggregatedFile));
						String line;
						while ((line = currentTempResult2ndReader.readLine()) != null) {
							String[] lineSplit = line.split(this.delimiter);
							String wordsWithoutFirst = LineFormatter
									.removeWord(lineSplit[0], 0);
							double lowOrderResult = Double
									.parseDouble(currentLowOrderResultReader
											.getLine(wordsWithoutFirst).split(
													this.delimiter)[1]);
							aggregated2ndResultWriter.write(line
									+ this.delimiter + lowOrderResult + "\n");
						}
						currentTempResult2ndReader.close();
						currentLowOrderResultReader.close();
						aggregated2ndResultWriter.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				if (this.deleteTempFiles) {
					try {
						FileUtils
								.deleteDirectory(currentTempResult2ndDirectory);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				// sort aggregated result by first column

				// initialize executerService
				executorService = Executors.newFixedThreadPool(cores);

				pipedInputStream = new PipedInputStream(100 * 8 * 1024);

				// don't add tags here
				splitterTask = new SplitterTask(pipedInputStream,
						this.tempResultDirectory, this.wordIndex,
						currentPattern, currentTempResultDirectory.getName(),
						this.delimiter, 0, this.deleteTempFiles, "", "", true);
				executorService.execute(splitterTask);

				try {
					OutputStream pipedOutputStream = new PipedOutputStream(
							pipedInputStream);

					SequenceModifier sequenceModifier = new SequenceModifier(
							currentTempResult2ndAggregatedDirectory,
							pipedOutputStream, this.delimiter, null, false,
							false);
					executorService.execute(sequenceModifier);

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				executorService.shutdown();
				try {
					executorService.awaitTermination(Long.MAX_VALUE,
							TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				if (this.deleteTempFiles) {
					try {
						FileUtils
								.deleteDirectory(currentTempResult2ndAggregatedDirectory);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			} else {
				// lower order pattern with same length as currentPattern
			}

		}
		// aggregate result files?
	}

}
