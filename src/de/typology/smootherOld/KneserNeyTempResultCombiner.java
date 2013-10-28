package de.typology.smootherOld;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

import de.typology.utils.Config;
import de.typology.utilsOld.IOHelper;

public class KneserNeyTempResultCombiner {
	protected File directory;
	protected File outputDirectory;
	protected String indexName;
	protected String statsName;
	protected BufferedReader tempResultReverseSortReader;
	protected SlidingWindowReader previousResultReverseSortReader;
	protected BufferedWriter reverseSortResultWriter;
	private KneserNeyFormatter kneserNeyFormatter;

	public KneserNeyTempResultCombiner(File directory, File outputDirecotry,
			String indexName, String statsName) {
		this.directory = directory;
		this.outputDirectory = outputDirecotry;
		this.indexName = indexName;
		this.statsName = statsName;
		this.kneserNeyFormatter = new KneserNeyFormatter();
	}

	// /**
	// * @param args
	// */
	// public static void main(String[] args) {
	// // TODO Auto-generated method stub
	//
	// }

	public void combine(String typeExtension, String lowExtension,
			String tempExtension, String revExtension, int maxSequenceLength,
			boolean calculateLog) {
		String outputDirectoryName = this.outputDirectory.getName();
		// revert sort temp directory
		SortSplitter tempSortSplitter = new SortSplitter(
				this.directory.getAbsolutePath() + "/", outputDirectoryName
						+ typeExtension + tempExtension, outputDirectoryName
						+ typeExtension + tempExtension + revExtension,
				this.indexName, this.statsName, "", true);
		tempSortSplitter.split(maxSequenceLength);

		// aggregate lower order results
		for (int sequenceDecimal = 1; sequenceDecimal < Math.pow(2,
				maxSequenceLength); sequenceDecimal++) {
			String sequenceBinary = Integer.toBinaryString(sequenceDecimal);
			// skip even results (since there is no target)
			if (sequenceDecimal % 2 == 0) {
				continue;
			}
			IOHelper.strongLog("calculating " + typeExtension.substring(1)
					+ " order kneser-ney value for sequence " + sequenceBinary);
			// aggregate revert sort temp result and previous result
			File currentTempReverseSortResultDirectory = new File(
					this.outputDirectory.getAbsolutePath() + typeExtension
							+ tempExtension + revExtension + "/"
							+ sequenceBinary);
			File currentReverseSortResultDirectory = new File(
					this.outputDirectory.getAbsolutePath() + typeExtension
							+ revExtension + "/" + sequenceBinary);
			if (sequenceDecimal == 1) {
				File currentLowTempReverseSortDirectory = new File(
						this.outputDirectory.getAbsolutePath() + lowExtension
								+ tempExtension + revExtension + "/"
								+ sequenceBinary);
				this.aggregateLowestOrder(sequenceBinary,
						currentLowTempReverseSortDirectory,
						currentReverseSortResultDirectory, calculateLog);
			} else {
				File previousReverseSortResultParentDirectory = new File(
						this.outputDirectory.getAbsolutePath() + lowExtension
								+ revExtension);
				this.aggregateResults(sequenceBinary,
						currentReverseSortResultDirectory,
						currentTempReverseSortResultDirectory,
						previousReverseSortResultParentDirectory, calculateLog);
			}
		}

		SortSplitter resultSortSplitter = new SortSplitter(
				this.directory.getAbsolutePath() + "/", outputDirectoryName
						+ typeExtension + revExtension, outputDirectoryName
						+ typeExtension, this.indexName, this.statsName, "",
				false);
		resultSortSplitter.split(maxSequenceLength);
	}

	private void aggregateResults(String sequenceBinary,
			File currentReverseSortResultDirectory,
			File currentTempReverseSortResultDirectory,
			File previousReverseSortResultParentDirectory, boolean calculateLog) {
		// for sequences > 1
		for (File currentTempResultRevSortFile : currentTempReverseSortResultDirectory
				.listFiles()) {
			String currentTempResultRevSortFileName = currentTempResultRevSortFile
					.getName().split("\\.")[0];
			currentReverseSortResultDirectory.mkdirs();

			this.initializeTempResultReverseSortReader(
					currentTempReverseSortResultDirectory.getParentFile(),
					previousReverseSortResultParentDirectory, sequenceBinary,
					currentTempResultRevSortFileName);

			currentReverseSortResultDirectory.mkdirs();
			this.reverseSortResultWriter = IOHelper.openWriteFile(
					currentReverseSortResultDirectory.getAbsolutePath() + "/"
							+ currentTempResultRevSortFileName + "."
							+ sequenceBinary,
					Config.get().memoryLimitForWritingFiles);
			String revSortLine;
			try {
				try {
					while ((revSortLine = this.tempResultReverseSortReader
							.readLine()) != null) {
						String[] revSortLineSplit = revSortLine.split("\t");
						String revSortWordsWithoutFirst = "";
						for (int i = 1; i < revSortLineSplit.length - 2; i++) {
							revSortWordsWithoutFirst += revSortLineSplit[i]
									+ "\t";
						}
						String revSortWords = revSortLineSplit[0] + "\t"
								+ revSortWordsWithoutFirst;
						double currentFirstFraction = Double
								.parseDouble(revSortLineSplit[revSortLineSplit.length - 2]);
						double currentDiscountFraction = Double
								.parseDouble(revSortLineSplit[revSortLineSplit.length - 1]);
						double previousResult = this.previousResultReverseSortReader
								.getCount(revSortWordsWithoutFirst);

						double result = currentFirstFraction
								+ currentDiscountFraction * previousResult;
						if (calculateLog) {
							result = Math.log10(result);
						}
						// System.out.println(revSortWords +
						// currentFirstFraction
						// + "+" + currentDiscountFraction + "*"
						// + previousResult);
						// this.reverseSortResultWriter.write(revSortWords
						// + KneserNeyFormatter.getRoundedResult(Math
						// .log10(result)) + "\n");
						this.reverseSortResultWriter.write(revSortWords
								+ this.kneserNeyFormatter
										.getRoundedResult(result) + "\n");
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			} finally {
				this.closeTempResultReverseSortReader();
				try {
					this.reverseSortResultWriter.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	private void aggregateLowestOrder(String sequenceBinary,
			File currentTempReverseSortResultDirectory,
			File currentReverseSortResultDirectory, boolean calculateLog) {
		// for sequences > 1
		for (File currentTempResultRevSortFile : currentTempReverseSortResultDirectory
				.listFiles()) {
			String currentTempResultRevSortFileName = currentTempResultRevSortFile
					.getName().split("\\.")[0];
			currentReverseSortResultDirectory.mkdirs();
			this.tempResultReverseSortReader = IOHelper.openReadFile(
					currentTempResultRevSortFile.getAbsolutePath(),
					Config.get().memoryLimitForReadingFiles / 2);
			this.reverseSortResultWriter = IOHelper.openWriteFile(
					currentReverseSortResultDirectory.getAbsolutePath() + "/"
							+ currentTempResultRevSortFileName + "."
							+ sequenceBinary,
					Config.get().memoryLimitForWritingFiles);
			String revSortLine;
			try {
				try {
					while ((revSortLine = this.tempResultReverseSortReader
							.readLine()) != null) {
						String[] revSortLineSplit = revSortLine.split("\t");
						double result = Double.parseDouble(revSortLineSplit[1]);
						if (calculateLog) {
							result = Math.log10(result);
						}
						this.reverseSortResultWriter.write(revSortLineSplit[0]
								+ "\t"
								+ this.kneserNeyFormatter
										.getRoundedResult(result) + "\n");
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			} finally {
				try {
					this.tempResultReverseSortReader.close();
					this.reverseSortResultWriter.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	private void initializeTempResultReverseSortReader(
			File tempResultReverseSortDirectory,
			File previousTempResultReverseSortParentDirectory,
			String sequenceBinary, String currentFileName) {
		int memoryLimitForReadingFiles = Config.get().memoryLimitForReadingFiles;
		memoryLimitForReadingFiles = memoryLimitForReadingFiles / 2;
		this.tempResultReverseSortReader = IOHelper.openReadFile(
				tempResultReverseSortDirectory.getAbsolutePath() + "/"
						+ sequenceBinary + "/" + currentFileName + "."
						+ sequenceBinary, memoryLimitForReadingFiles);

		String previousResultReverseSortSequence = sequenceBinary.substring(1);
		// skip wildcard word in previous result
		// e.g.: 01 --> 1 or 001 --> 1
		if (previousResultReverseSortSequence.startsWith("0")) {
			previousResultReverseSortSequence = previousResultReverseSortSequence
					.replaceFirst("0*", "");
		}
		this.previousResultReverseSortReader = new SlidingWindowReader(
				previousTempResultReverseSortParentDirectory.getAbsolutePath()
						+ "/" + previousResultReverseSortSequence + "/"
						+ currentFileName + "."
						+ previousResultReverseSortSequence,
				memoryLimitForReadingFiles);
	}

	private void closeTempResultReverseSortReader() {
		try {
			this.tempResultReverseSortReader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.previousResultReverseSortReader.close();
	}

}
