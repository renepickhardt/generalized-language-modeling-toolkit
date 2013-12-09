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
import java.util.Arrays;
import java.util.HashMap;
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
import de.typology.utils.SequenceFormatter;
import de.typology.utils.SlidingWindowReader;

public class LargeKneserNeyResultAggregator {
	private File lowOrderResultDirectory;
	private File tempResultDirectory;

	private WordIndex wordIndex;
	private String delimiter;
	private DecimalFormatter decimalFormatter;
	private boolean deleteTempFiles;

	private Logger logger = LogManager.getLogger(this.getClass().getName());

	public LargeKneserNeyResultAggregator(File lowOrderResultDirectory,
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
			ArrayList<Integer> removeBitPositions, int cores) {
		ExecutorService executorService;
		int currentTempDirNumber = 1;
		for (int removeBitPosition : removeBitPositions) {

			// build backoffPattern
			boolean[] backoffPattern;
			if (removeBitPosition == 0) {
				backoffPattern = Arrays.copyOfRange(currentPattern, 1,
						currentPattern.length);
				while (!backoffPattern[0] && backoffPattern.length > 1) {
					backoffPattern = Arrays.copyOfRange(backoffPattern, 1,
							backoffPattern.length);
				}
			} else {
				backoffPattern = currentPattern.clone();
				backoffPattern[removeBitPosition] = false;
			}

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
				// aggregate results for lower order pattern with first word
				// missing
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
						this.delimiter, 1, this.deleteTempFiles, "", "", true,
						true);
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
							String wordsWithoutFirst = SequenceFormatter
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
						this.delimiter, 0, this.deleteTempFiles, "", "", true,
						true);
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
				for (File currentTempResultFile : currentTempResultDirectory
						.listFiles()) {
					// load low order lines into HashSet
					HashMap<String, Double> lowerOrderResultMap = new HashMap<String, Double>();
					File currentLowOrderResultFile = new File(
							currentLowOrderResultDirectory.getAbsolutePath()
									+ "/" + currentTempResultFile.getName());
					try {
						BufferedReader currentLowOrderResultReader = new BufferedReader(
								new FileReader(currentLowOrderResultFile));
						String line;
						while ((line = currentLowOrderResultReader.readLine()) != null) {
							String[] lineSplit = line.split(this.delimiter);
							lowerOrderResultMap.put(lineSplit[0],
									Double.parseDouble(lineSplit[1]));
						}
						currentLowOrderResultReader.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					File previousTempResultFile = new File(
							this.tempResultDirectory.getAbsolutePath()
									+ "/"
									+ PatternTransformer
											.getStringPattern(currentPattern)
									+ "-" + (currentTempDirNumber - 1));
					if (!previousTempResultFile.exists()) {
						this.logger.error("previous result not found: "
								+ previousTempResultFile.getAbsolutePath());
						System.exit(1);
					}
					try {
						BufferedReader previousTempResultReader = new BufferedReader(
								new FileReader(previousTempResultFile));
						BufferedWriter aggregatedResultWriter = new BufferedWriter(
								new FileWriter(currentTempResultFile));
						String previousLine;
						while ((previousLine = previousTempResultReader
								.readLine()) != null) {
							double lowerOrderResult = lowerOrderResultMap
									.get(SequenceFormatter.removeWord(
											previousLine.split(this.delimiter)[0],
											removeBitPosition));
							aggregatedResultWriter.write(previousLine
									+ this.delimiter + lowerOrderResult + "\n");

						}
						previousTempResultReader.close();
						aggregatedResultWriter.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}

			}

		}
		// aggregate result files?
	}
}
