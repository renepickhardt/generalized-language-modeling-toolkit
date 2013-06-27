package de.typology.smoother;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;

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
				"kneser-ney", "index.txt", "stats.txt");
		kna.calculate(5);
	}

	protected String directory;
	protected File absoluteDirectory;
	protected File _absoluteDirectory;
	protected File absolute_Directory;
	protected File _absolute_Directory;
	protected File outputDirectory;
	protected File tempResultDirectory;
	protected File tempReverseSortDirectory;

	protected BufferedReader absoluteReader;
	protected BufferedReader _absoluteReader;
	protected SlidingWindowReader absolute_Reader;
	protected SlidingWindowReader _absolute_Reader;
	protected SlidingWindowReader absoluteWithoutLastReader;

	protected SlidingWindowReader tempResultReverseSortReader;
	protected SlidingWindowReader previousTempResultReverseSortReader;

	protected BufferedWriter tempResultWriter;
	protected BufferedWriter reverseSortResultWriter;
	protected SortSplitter reverseSortSplitter;

	private String indexName;
	private String statsName;

	private double d1plus;
	DecimalFormat decimalFormat = new DecimalFormat("###.######");

	public KneserNeyAggregator(String directory, String absoluteDirectoryName,
			String _absoluteDirectoryName, String absolute_DirectoryName,
			String _absolute_DirectoryName, String outputDirectoryName,
			String indexName, String statsName) {
		this.indexName = indexName;
		this.statsName = statsName;
		this.directory = directory;
		this.absoluteDirectory = new File(this.directory
				+ absoluteDirectoryName);
		this._absoluteDirectory = new File(this.directory
				+ _absoluteDirectoryName);
		this.absolute_Directory = new File(this.directory
				+ absolute_DirectoryName);
		this._absolute_Directory = new File(this.directory
				+ _absolute_DirectoryName);
		this.outputDirectory = new File(this.directory + outputDirectoryName);
		this.tempResultDirectory = new File(this.directory
				+ outputDirectoryName + "-temp");
		this.tempReverseSortDirectory = new File(this.directory
				+ outputDirectoryName + "-temp-rev");

		try {
			FileUtils.deleteDirectory(this.outputDirectory);
			FileUtils.deleteDirectory(this.tempResultDirectory);
			FileUtils.deleteDirectory(this.tempReverseSortDirectory);
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.outputDirectory.mkdir();
		this.tempResultDirectory.mkdir();
		this.tempReverseSortDirectory.mkdir();

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
			// skip even results (since there is no target)
			if (sequenceDecimal % 2 == 0) {
				continue;
			}
			IOHelper.strongLog("calculating sequence " + sequenceBinary);
			String currentAbsolteDirectory = this.absoluteDirectory + "/"
					+ sequenceBinary;
			this.calculateDs(currentAbsolteDirectory);
			boolean sequenceIsMaxLength = sequenceBinary.length() == maxSequenceLength;
			if (sequenceIsMaxLength) {
				for (File absoluteFile : new File(currentAbsolteDirectory)
						.listFiles()) {
					String absoluteFileName = absoluteFile.getName().split(
							"\\.")[0];
					this.initializeAbsoluteReaders(sequenceBinary,
							absoluteFileName);
					this.initializeAbsolute_Reader(sequenceBinary,
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
								if (absoluteLineSplit[1].equals("<s>")) {
									// skip <s> <s>
									continue;
								}
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
										* this.getAbsolute_Count(absoluteWordsWithoutLast);
								this.tempResultWriter.write(absoluteWords
										+ firstFractionResult + "\t"
										+ discountValueResult + "\n");
							}
						} finally {
							this.closeAbsoluteReaders();
							this.closeAbsolute_Reader();
							this.closeTempResultWriter();
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} else {
				// sequenceLength>maxLength
				String _absoluteSequence = "_" + sequenceBinary;
				for (File _absoluteFile : new File(this._absoluteDirectory
						+ "/" + _absoluteSequence).listFiles()) {
					String _absoluteFileName = _absoluteFile.getName().split(
							"\\.")[0];
					this.initializeContinuationReaders(_absoluteSequence,
							_absoluteFileName);
					if (sequenceDecimal > 1) {
						this.initializeAbsolute_Reader(sequenceBinary,
								_absoluteFileName);
					}
					this.initializeTempResultWriter(sequenceBinary,
							_absoluteFileName);

					String _absoluteLine;
					try {
						try {
							while ((_absoluteLine = this._absoluteReader
									.readLine()) != null) {
								String[] _absoluteLineSplit = _absoluteLine
										.split("\t");
								String _absoluteWords = "";
								for (int i = 0; i < _absoluteLineSplit.length - 1; i++) {
									_absoluteWords += _absoluteLineSplit[i]
											+ "\t";
								}
								String _absoluteWordsWithoutLast = "";
								for (int i = 0; i < _absoluteLineSplit.length - 2; i++) {
									_absoluteWordsWithoutLast += _absoluteLineSplit[i]
											+ "\t";
								}

								int _absoluteCount = Integer
										.parseInt(_absoluteLineSplit[_absoluteLineSplit.length - 1]);
								long _absolute_Count = this.get_absolute_Count(
										_absoluteWordsWithoutLast,
										this._absolute_Directory
												.getAbsolutePath()
												+ "/"
												+ _absoluteSequence.substring(
														0, _absoluteSequence
																.length() - 1)
												+ "_");
								if (sequenceDecimal == 1) {
									if (_absoluteWords.startsWith("<s>")) {
										this.tempResultWriter
												.write(_absoluteWords + "-99\n");
										continue;
									}
									// calculate first fraction of the equation
									double kneserNeyResult = (double) _absoluteCount
											/ _absolute_Count;
									// the result is already reverse sorted
									// since there is only one row
									System.out.println(_absoluteWords + ": "
											+ _absoluteCount + " / "
											+ _absolute_Count);
									this.tempResultWriter
											.write(_absoluteWords
													+ this.getRoundedResult(kneserNeyResult)
													+ "\n");

								} else {
									// calculate first fraction of the
									// equation
									double continuationMinusDResult = _absoluteCount
											- this.getD(_absoluteCount);
									if (continuationMinusDResult < 0) {
										continuationMinusDResult = 0;
									}
									double firstFractionResult = continuationMinusDResult
											/ _absolute_Count;

									// calculate the discount value
									double discountFractionResult = this
											.getD(_absoluteCount)
											/ _absolute_Count;
									double discountValueResult = discountFractionResult
											* this.getAbsolute_Count(_absoluteWordsWithoutLast);
									this.tempResultWriter.write(_absoluteWords
											+ firstFractionResult + "\t"
											+ discountValueResult + "\n");
								}
							}
						} finally {
							this.closeContinuationReaders();
							if (sequenceDecimal > 1) {
								this.closeAbsolute_Reader();
							}
							this.closeTempResultWriter();
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}

		// revert sort temp result
		SortSplitter revertSortSplitter = new SortSplitter(this.directory,
				this.tempResultDirectory.getName(),
				this.tempReverseSortDirectory.getName(), this.indexName,
				this.statsName, "", true);
		revertSortSplitter.split(maxSequenceLength);

		for (int sequenceDecimal = 1; sequenceDecimal < Math.pow(2,
				maxSequenceLength); sequenceDecimal++) {
			String sequenceBinary = Integer.toBinaryString(sequenceDecimal);
			// skip even results (since there is no target)
			if (sequenceDecimal % 2 == 0) {
				continue;
			}
			IOHelper.strongLog("calculating sequence " + sequenceBinary);
			// aggregate revert sort temp result and previous result
			// TODO:add aggregation here
		}
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
				sequenceBinary.length() - 1);
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

		this._absoluteReader = IOHelper.openReadFile(
				this._absoluteDirectory.getAbsolutePath() + "/"
						+ continuationSequence + "/" + currentFileName + "."
						+ continuationSequence, memoryLimitForReadingFiles);

		String nContinuationSequence = continuationSequence.substring(0,
				continuationSequence.length() - 1) + "_";
		this._absolute_Reader = new SlidingWindowReader(
				this._absolute_Directory.getAbsolutePath() + "/"
						+ nContinuationSequence + "/" + currentFileName + "."
						+ nContinuationSequence, memoryLimitForReadingFiles);
	}

	private void closeContinuationReaders() {
		try {
			this._absoluteReader.close();
			this._absolute_Reader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void initializeAbsolute_Reader(String continuationSequence,
			String currentFileName) {
		int memoryLimitForReadingFiles = Config.get().memoryLimitForReadingFiles;
		memoryLimitForReadingFiles = memoryLimitForReadingFiles / 4;

		String absolute_Sequence = continuationSequence.substring(0,
				continuationSequence.length() - 1) + "_";
		this.absolute_Reader = new SlidingWindowReader(
				this.absolute_Directory.getAbsolutePath() + "/"
						+ absolute_Sequence + "/" + currentFileName + "."
						+ absolute_Sequence, memoryLimitForReadingFiles);

	}

	private void closeAbsolute_Reader() {
		this.absolute_Reader.close();
	}

	private void initializeTempResultReverseSortReader(String sequenceBinary,
			String currentFileName) {
		int memoryLimitForReadingFiles = Config.get().memoryLimitForReadingFiles;
		memoryLimitForReadingFiles = memoryLimitForReadingFiles / 2;
		this.tempResultReverseSortReader = new SlidingWindowReader(
				this.tempResultDirectory.getAbsolutePath() + "/"
						+ sequenceBinary + "/" + currentFileName + "."
						+ sequenceBinary, memoryLimitForReadingFiles);

		String previousResultReverseSortSequence = sequenceBinary.substring(1);
		// skip wildcard word in previous result
		// e.g.: 01 --> 1 or 001 --> 1
		if (previousResultReverseSortSequence.startsWith("0")) {
			previousResultReverseSortSequence = previousResultReverseSortSequence
					.replaceFirst("0*", "");
		}
		this.previousTempResultReverseSortReader = new SlidingWindowReader(
				this.tempResultDirectory.getAbsolutePath() + "/"
						+ previousResultReverseSortSequence + "/"
						+ currentFileName + "."
						+ previousResultReverseSortSequence,
				memoryLimitForReadingFiles);
	}

	private void closeTempResultReverseSortReader() {
		this.tempResultReverseSortReader.close();
		this.previousTempResultReverseSortReader.close();
	}

	protected void initializeReverseSortResultWriter(String sequenceBinary,
			String currentFileName) {
		File currentReverseOutputDirectory = new File(
				this.tempReverseSortDirectory.getAbsolutePath() + "/"
						+ sequenceBinary);
		currentReverseOutputDirectory.mkdir();
		this.reverseSortResultWriter = IOHelper.openWriteFile(
				currentReverseOutputDirectory.getAbsolutePath() + "/"
						+ currentFileName + "." + sequenceBinary,
				Config.get().memoryLimitForWritingFiles);
	}

	private void closeReverseSortResultWriter() {
		try {
			this.reverseSortResultWriter.close();
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

	private void calculateDs(String directoryPath) {
		// TODO calculation
		long n1 = Counter.countCountsInDirectory(1, directoryPath);
		long n2 = Counter.countCountsInDirectory(2, directoryPath);
		System.out.println("n1: " + n1);
		System.out.println("n2: " + n2);
		this.d1plus = 0.5;
	}

	private double getD(int continuationCount) {
		return this.d1plus;
	}

	private long getAbsoluteWithoutLastCount(String absoluteWordsWithoutLast) {
		return this.absoluteWithoutLastReader
				.getCount(absoluteWordsWithoutLast);
	}

	private int getAbsolute_Count(String continuationWordsWithoutLast) {
		return this.absolute_Reader.getCount(continuationWordsWithoutLast);
	}

	private long get_absolute_Count(String continuationWordsWithoutLast,
			String currentNContinuationDirectory) {
		if (continuationWordsWithoutLast.isEmpty()) {
			return Counter.countColumnCountsInDirectory(0,
					currentNContinuationDirectory);
		} else {
			return this._absolute_Reader.getCount(continuationWordsWithoutLast);
		}
	}

	private String getRoundedResult(double input) {
		return this.decimalFormat.format(Math.log10(input));
	}
}
