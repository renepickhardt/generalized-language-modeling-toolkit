package de.typology.smoother;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import de.typology.utils.Config;
import de.typology.utils.Counter;
import de.typology.utils.IOHelper;

public class KneserNeyAggregator {
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String outputDirectory = Config.get().outputDirectory
				+ Config.get().inputDataSet;
		KneserNeyAggregator kna = new KneserNeyAggregator(outputDirectory,
				"absolute", "_absolute", "absolute_", "_absolute_",
				"kneser-ney");
		kna.calculate(5);
	}

	public String test;

	public void count(String test) {
		test = test + test;
	}

	protected String directory;
	protected File absoluteDirectory;
	protected File continuationDirectory;
	protected File nAbsoluteDirectory;
	protected File nContinuationDirectory;
	protected File outputDirectory;
	protected File tempResultDirectory;
	protected File reverseOutputDirectory;

	protected BufferedReader continuationReader;
	protected BufferedReader absoluteReader;
	protected SlidingWindowReader absoluteWithoutLastReader;
	protected SlidingWindowReader nAbsoluteReader;
	protected SlidingWindowReader nContinuationReader;
	protected SlidingWindowReader previousResultReverseSortReader;

	protected BufferedWriter tempResultWriter;
	protected BufferedWriter reverstSortResultWriter;
	protected RevertSortSplitter revertSortSplitter;

	private double d1plus;

	public KneserNeyAggregator(String directory, String absoluteDirectoryName,
			String continuationDirectoryName,
			String nAbsoluteReverseDirectoryName,
			String nContinuationDirectoryName, String outputDirectoryName) {
		this.directory = directory;
		this.absoluteDirectory = new File(this.directory
				+ absoluteDirectoryName);
		this.continuationDirectory = new File(this.directory
				+ continuationDirectoryName);
		this.nAbsoluteDirectory = new File(this.directory
				+ nAbsoluteReverseDirectoryName);
		this.nContinuationDirectory = new File(this.directory
				+ nContinuationDirectoryName);
		this.outputDirectory = new File(this.directory + outputDirectoryName);
		this.tempResultDirectory = new File(this.directory
				+ outputDirectoryName + "-temp");
		this.reverseOutputDirectory = new File(this.directory
				+ outputDirectoryName);
		if (this.outputDirectory.exists()) {
			try {
				FileUtils.deleteDirectory(this.outputDirectory);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (this.tempResultDirectory.exists()) {
			try {
				FileUtils.deleteDirectory(this.tempResultDirectory);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (this.reverseOutputDirectory.exists()) {
			try {
				FileUtils.deleteDirectory(this.reverseOutputDirectory);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		this.outputDirectory.mkdir();
		this.tempResultDirectory.mkdir();
		this.reverseOutputDirectory.mkdir();

		this.calculateDs();
	}

	private void initializeAbsoluteReaders(String sequenceBinary,
			String currentFileName) {
		int memoryLimitForReadingFiles = Config.get().memoryLimitForReadingFiles;
		memoryLimitForReadingFiles = memoryLimitForReadingFiles / 4;

		this.absoluteReader = IOHelper.openReadFile(
				this.absoluteDirectory.getAbsolutePath() + "/" + sequenceBinary
						+ "/" + currentFileName + "." + sequenceBinary,
				memoryLimitForReadingFiles);

		String sequenceBinaryWithoutLast = sequenceBinary.substring(0,
				sequenceBinary.length() - 2);
		this.absoluteWithoutLastReader = new SlidingWindowReader(
				this.absoluteDirectory.getAbsolutePath() + "/"
						+ sequenceBinaryWithoutLast + "/" + currentFileName
						+ "." + sequenceBinaryWithoutLast,
				memoryLimitForReadingFiles);
	}

	private void closeAbsoluteReaders() {
		try {
			this.absoluteReader.close();
			this.absoluteWithoutLastReader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void initializeContinuationReaders(String continuationSequence,
			String currentFileName) {
		int memoryLimitForReadingFiles = Config.get().memoryLimitForReadingFiles;
		memoryLimitForReadingFiles = memoryLimitForReadingFiles / 4;

		this.continuationReader = IOHelper.openReadFile(
				this.continuationDirectory.getAbsolutePath() + "/"
						+ continuationSequence + "/" + currentFileName + "."
						+ continuationSequence, memoryLimitForReadingFiles);

		String nContinuationSequence = continuationSequence.substring(0,
				continuationSequence.length() - 1) + "_";
		this.nContinuationReader = new SlidingWindowReader(
				this.nContinuationDirectory.getAbsolutePath() + "/"
						+ nContinuationSequence + "/" + currentFileName + "."
						+ nContinuationSequence, memoryLimitForReadingFiles);
	}

	private void closeContinuationReaders() {
		try {
			this.continuationReader.close();
			this.nContinuationReader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void initializeOtherReaders(String continuationSequence,
			String currentFileName) {
		int memoryLimitForReadingFiles = Config.get().memoryLimitForReadingFiles;
		memoryLimitForReadingFiles = memoryLimitForReadingFiles / 4;

		String nAbsoluteSequence = continuationSequence.substring(0,
				continuationSequence.length() - 2) + "_";
		this.nAbsoluteReader = new SlidingWindowReader(
				this.nAbsoluteDirectory.getAbsolutePath() + "/"
						+ nAbsoluteSequence + "/" + currentFileName + "."
						+ nAbsoluteSequence, memoryLimitForReadingFiles);

		String previousResultReverseSortSequence = continuationSequence
				.substring(1);
		this.previousResultReverseSortReader = new SlidingWindowReader(
				this.reverseOutputDirectory.getAbsolutePath() + "/"
						+ previousResultReverseSortSequence + "/"
						+ currentFileName + "."
						+ previousResultReverseSortSequence,
				memoryLimitForReadingFiles);
	}

	private void closeOtherReaders() {
		this.nAbsoluteReader.close();
		this.previousResultReverseSortReader.close();
	}

	protected void initializeReverseSortWriter(String sequenceBinary,
			String currentFileName) {
		File currentReverseOutputDirectory = new File(
				this.reverseOutputDirectory.getAbsolutePath() + "/"
						+ sequenceBinary);
		currentReverseOutputDirectory.mkdir();
		this.reverstSortResultWriter = IOHelper.openWriteFile(
				currentReverseOutputDirectory.getAbsolutePath() + "/"
						+ currentFileName + "." + sequenceBinary,
				Config.get().memoryLimitForWritingFiles);
	}

	private void closeReverseSortWriter() {
		try {
			this.reverstSortResultWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void initializeTempResultWriter(String sequenceBinary,
			String currentFileName) {
		File currentTempResultDirectory = new File(
				this.tempResultDirectory.getAbsolutePath() + "/"
						+ sequenceBinary);
		currentTempResultDirectory.mkdir();
		this.tempResultWriter = IOHelper.openWriteFile(
				currentTempResultDirectory.getAbsolutePath() + "/"
						+ currentFileName + "." + sequenceBinary,
				Config.get().memoryLimitForWritingFiles);
	}

	private void closeTempResultWriter() {
		try {
			this.tempResultWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param maxSequenceLength
	 *            needs to be greater than 1
	 */
	private void calculate(int maxSequenceLength) {
		IOHelper.strongLog("calcualting kneser-ney weights for "
				+ this.directory);

		for (int sequenceDecimal = 1; sequenceDecimal < Math.pow(2,
				maxSequenceLength); sequenceDecimal++) {
			String sequenceBinary = Integer.toBinaryString(sequenceDecimal);
			IOHelper.strongLog("calculating sequence " + sequenceBinary);
			// skip even results (since there is no target)
			if (sequenceDecimal % 2 == 0) {
				continue;
			}
			boolean sequenceIsMaxLength = sequenceBinary.length() == maxSequenceLength;
			if (sequenceIsMaxLength) {
				for (File absoluteFile : new File(this.absoluteDirectory + "/"
						+ sequenceBinary).listFiles()) {
					String absoluteFileName = absoluteFile.getName().split(
							"\\.")[0];
					this.initializeAbsoluteReaders(sequenceBinary,
							absoluteFileName);
					this.initializeOtherReaders(sequenceBinary,
							absoluteFileName);
					this.initializeTempResultWriter(sequenceBinary,
							absoluteFileName);
					String absoluteLine;
					try {
						try {
							while ((absoluteLine = this.absoluteReader
									.readLine()) != null) {
								String[] absoluteLineSplit = absoluteLine
										.split("\t");
								String absoluteWords = "";
								for (int i = 0; i < absoluteLineSplit.length - 1; i++) {
									absoluteWords += absoluteLineSplit[i]
											+ "\t";
								}
								String absoluteWordsWithoutLast = "";
								for (int i = 0; i < absoluteLineSplit.length - 2; i++) {
									absoluteWordsWithoutLast += absoluteLineSplit[i]
											+ "\t";
								}

								// calculate first fraction of the
								// equation
								int absoluteCount = Integer
										.parseInt(absoluteLineSplit[absoluteLineSplit.length - 1]);
								double absoluteMinusDResult = absoluteCount
										- this.getD(absoluteCount);
								if (absoluteMinusDResult < 0) {
									absoluteMinusDResult = 0;
								}
								long absoluteWithoutLastCount = this
										.getAbsoluteWithoutLastCount(absoluteWordsWithoutLast);
								double firstFractionResult = absoluteMinusDResult
										/ absoluteWithoutLastCount;

								// calculate the discount value
								double discountFractionResult = this
										.getD(absoluteCount)
										/ absoluteWithoutLastCount;
								double discountValueResult = discountFractionResult
										* this.getNAbsoluteCount(absoluteWordsWithoutLast);
								this.tempResultWriter.write(absoluteWords
										+ firstFractionResult + "\t"
										+ discountValueResult + "\n");
							}
						} finally {
							this.closeAbsoluteReaders();
							this.closeOtherReaders();
							this.closeTempResultWriter();
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} else {
				String continuationSequence = "_" + sequenceBinary;
				for (File continuationFile : new File(
						this.continuationDirectory + "/" + continuationSequence)
						.listFiles()) {
					String continuationFileName = continuationFile.getName()
							.split("\\.")[0];
					this.initializeContinuationReaders(continuationSequence,
							continuationFileName);
					if (sequenceDecimal == 1) {
						this.initializeReverseSortWriter(sequenceBinary,
								continuationFileName);
					} else {
						this.initializeOtherReaders(sequenceBinary,
								continuationFileName);
						this.initializeTempResultWriter(sequenceBinary,
								continuationFileName);
					}
					String continuationLine;
					try {
						try {
							while ((continuationLine = this.continuationReader
									.readLine()) != null) {
								String[] continuationLineSplit = continuationLine
										.split("\t");
								String continuationWords = "";
								for (int i = 0; i < continuationLineSplit.length - 1; i++) {
									continuationWords += continuationLineSplit[i]
											+ "\t";
								}
								String continuationWordsWithoutLast = "";
								for (int i = 0; i < continuationLineSplit.length - 2; i++) {
									continuationWordsWithoutLast += continuationLineSplit[i]
											+ "\t";
								}

								int continuationCount = Integer
										.parseInt(continuationLineSplit[continuationLineSplit.length - 1]);
								long nContinuationCount = this
										.getNContinuationCount(
												continuationWordsWithoutLast,
												this.nContinuationDirectory
														.getAbsolutePath()
														+ "/"
														+ continuationSequence
																.substring(
																		0,
																		continuationSequence
																				.length() - 1)
														+ "_");
								if (sequenceDecimal == 1) {
									// calculate first fraction of the equation
									double kneserNeyResult = continuationCount
											/ nContinuationCount;
									// the result is already reverse sorted
									// since there is only one row
									this.reverstSortResultWriter
											.write(continuationWords
													+ kneserNeyResult + "\n");

								} else {
									// calculate first fraction of the
									// equation
									double continuationMinusDResult = continuationCount
											- this.getD(continuationCount);
									if (continuationMinusDResult < 0) {
										continuationMinusDResult = 0;
									}
									double firstFractionResult = continuationMinusDResult
											/ nContinuationCount;

									// calculate the discount value
									double discountFractionResult = this
											.getD(continuationCount)
											/ nContinuationCount;
									double discountValueResult = discountFractionResult
											* this.getNAbsoluteCount(continuationWordsWithoutLast);
									this.tempResultWriter
											.write(continuationWords
													+ firstFractionResult
													+ "\t"
													+ discountValueResult
													+ "\n");
								}
							}
						} finally {
							this.closeContinuationReaders();
							if (sequenceDecimal == 1) {
								this.closeReverseSortWriter();
							} else {
								this.closeOtherReaders();
								this.closeTempResultWriter();
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

			// aggregate temp result and previous result
			if (sequenceDecimal == 1) {
				// no aggregation necessary
				continue;
			}
		}
	}

	private void calculateDs() {
		// TODO calculation
		this.d1plus = 0.5;
	}

	private double getD(int continuationCount) {
		return this.d1plus;
	}

	private long getAbsoluteWithoutLastCount(String absoluteWordsWithoutLast) {
		return this.absoluteWithoutLastReader
				.getCount(absoluteWordsWithoutLast);
	}

	private int getNAbsoluteCount(String continuationWordsWithoutLast) {
		return this.nAbsoluteReader.getCount(continuationWordsWithoutLast);
	}

	private long getNContinuationCount(String continuationWordsWithoutLast,
			String currentNContinuationDirectory) {
		if (continuationWordsWithoutLast.isEmpty()) {
			return Counter.countColumnCountsInDirectory(0,
					currentNContinuationDirectory);
		} else {
			return this.nContinuationReader
					.getCount(continuationWordsWithoutLast);
		}
	}

}
