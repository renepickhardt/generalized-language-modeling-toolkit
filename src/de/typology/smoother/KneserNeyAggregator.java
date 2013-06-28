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
	protected File lowTempResultDirectory;
	protected File absTempResultDirectory;
	protected File lowTempReverseSortDirectory;
	protected File absTempReverseSortDirectory;

	protected BufferedReader absoluteReader;
	protected BufferedReader _absoluteReader;
	protected SlidingWindowReader absolute_Reader;
	protected SlidingWindowReader _absolute_Reader;
	protected SlidingWindowReader absoluteWithoutLastReader;

	protected BufferedReader tempResultReverseSortReader;
	protected SlidingWindowReader previousResultReverseSortReader;

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
		this.lowTempResultDirectory = new File(this.directory
				+ outputDirectoryName + "-low-temp");
		this.absTempResultDirectory = new File(this.directory
				+ outputDirectoryName + "-abs-temp");
		this.lowTempReverseSortDirectory = new File(this.directory
				+ outputDirectoryName + "-low-temp-rev");
		this.absTempReverseSortDirectory = new File(this.directory
				+ outputDirectoryName + "-abs-temp-rev");

		try {
			FileUtils.deleteDirectory(this.outputDirectory);
			FileUtils.deleteDirectory(this.lowTempResultDirectory);
			FileUtils.deleteDirectory(this.absTempResultDirectory);
			FileUtils.deleteDirectory(this.lowTempReverseSortDirectory);
			FileUtils.deleteDirectory(this.absTempReverseSortDirectory);
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.outputDirectory.mkdir();
		this.lowTempResultDirectory.mkdir();
		this.absTempResultDirectory.mkdir();
		this.lowTempReverseSortDirectory.mkdir();
		this.absTempReverseSortDirectory.mkdir();

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
			IOHelper.strongLog("calculating absolute sequence "
					+ sequenceBinary);
			String currentAbsolteDirectory = this.absoluteDirectory + "/"
					+ sequenceBinary;
			this.calculateDs(currentAbsolteDirectory);
			// build absolute results
			if (sequenceDecimal > 1) {
				for (File absoluteFile : new File(currentAbsolteDirectory)
						.listFiles()) {
					String absoluteFileName = absoluteFile.getName().split(
							"\\.")[0];
					this.initializeAbsoluteReaders(sequenceBinary,
							absoluteFileName);
					this.initializeAbsolute_Reader(sequenceBinary,
							absoluteFileName);
					this.initializeTempResultAbsoluteWriter(sequenceBinary,
							absoluteFileName);
					String absoluteLine;
					try {
						try {
							while ((absoluteLine = this.absoluteReader
									.readLine()) != null) {
								String[] absoluteLineSplit = absoluteLine
										.split("\t");
								if (absoluteLine.startsWith("<fs>")) {
									// skip <fs>
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
								double absoluteWithoutLastCount = this
										.getAbsoluteWithoutLastCount(absoluteWordsWithoutLast);
								double firstFractionResult = absoluteMinusDResult
										/ absoluteWithoutLastCount;

								// System.out.println(absoluteWords + ": "
								// + absoluteMinusDResult + "/"
								// + absoluteWithoutLastCount);
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
							this.closeAbsTempResultWriter();
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			// sequenceLength<maxLength
			if (sequenceBinary.length() < maxSequenceLength) {
				IOHelper.strongLog("calculating lower order sequence "
						+ sequenceBinary);
				String _absoluteSequence = "_" + sequenceBinary;
				for (File _absoluteFile : new File(this._absoluteDirectory
						+ "/" + _absoluteSequence).listFiles()) {
					String _absoluteFileName = _absoluteFile.getName().split(
							"\\.")[0];
					this.initialize_absolteReaders(_absoluteSequence,
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
								double _absolute_Count = this
										.get_absolute_Count(
												_absoluteWordsWithoutLast,
												this._absolute_Directory
														.getAbsolutePath()
														+ "/"
														+ _absoluteSequence
																.substring(
																		0,
																		_absoluteSequence
																				.length() - 1)
														+ "_");
								if (sequenceDecimal == 1) {
									if (_absoluteWords.startsWith("<s>")) {
										// this.tempResultWriter
										// .write(_absoluteWords + "-99\n");
										continue;
									}
									// calculate first fraction of the equation
									double kneserNeyResult = _absoluteCount
											/ _absolute_Count;
									// the result is already reverse sorted
									// since there is only one row
									// System.out.println(_absoluteWords + ": "
									// + _absoluteCount + " / "
									// + _absolute_Count);
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
											.getDNumerator(_absoluteCount,
													_absoluteWordsWithoutLast)
											/ _absolute_Count;
									// System.out.println(_absoluteWords + ": "
									// + continuationMinusDResult + " / "
									// + _absolute_Count);
									this.tempResultWriter.write(_absoluteWords
											+ firstFractionResult + "\t"
											+ discountFractionResult + "\n");
								}
							}
						} finally {
							this.close_absoluteReaders();
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

		// revert sort low temp result
		SortSplitter lowTempSortSplitter = new SortSplitter(this.directory,
				this.lowTempResultDirectory.getName(),
				this.lowTempReverseSortDirectory.getName(), this.indexName,
				this.statsName, "", true);
		lowTempSortSplitter.split(maxSequenceLength);
		// revert sort abs temp result
		SortSplitter absTempSortSplitter = new SortSplitter(this.directory,
				this.absTempResultDirectory.getName(),
				this.absTempReverseSortDirectory.getName(), this.indexName,
				this.statsName, "", true);
		absTempSortSplitter.split(maxSequenceLength);

		// aggregate lower order results
		for (int sequenceDecimal = 2; sequenceDecimal < Math.pow(2,
				maxSequenceLength - 1); sequenceDecimal++) {
			String sequenceBinary = Integer.toBinaryString(sequenceDecimal);
			// skip even results (since there is no target)
			if (sequenceDecimal % 2 == 0) {
				continue;
			}
			IOHelper.strongLog("calculating lower order kneser-ney value for sequence "
					+ sequenceBinary);
			// aggregate revert sort temp result and previous result
			File currentTempReverseSortResultDirectory = new File(
					this.outputDirectory.getAbsolutePath() + "-low-temp-rev/"
							+ sequenceBinary);
			File currentReverseSortResultDirectory = new File(
					this.outputDirectory.getAbsolutePath() + "-low-rev/"
							+ sequenceBinary);

			// for sequences > 1
			for (File currentTempResultRevSortFile : currentTempReverseSortResultDirectory
					.listFiles()) {
				String currentTempResultRevSortFileName = currentTempResultRevSortFile
						.getName().split("\\.")[0];
				currentReverseSortResultDirectory.mkdirs();
				this.initializeTempResultReverseSortReader(sequenceBinary,
						currentTempResultRevSortFileName);
				this.initializeReverseSortResultWriter(sequenceBinary,
						currentTempResultRevSortFileName);
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
							System.out.println(revSortWords
									+ currentFirstFraction + "+"
									+ currentDiscountFraction + "*"
									+ previousResult);
							this.reverseSortResultWriter.write(revSortWords
									+ this.getRoundedResult(Math.log10(result))
									+ "\n");
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				} finally {
					this.closeTempResultReverseSortReader();
					this.closeReverseSortResultWriter();
				}

			}
		}

		for (int sequenceDecimal = 2; sequenceDecimal < Math.pow(2,
				maxSequenceLength - 1); sequenceDecimal++) {
			String sequenceBinary = Integer.toBinaryString(sequenceDecimal);
			// skip even results (since there is no target)
			if (sequenceDecimal % 2 == 0) {
				continue;
			}
			IOHelper.strongLog("calculating final kneser-ney value for sequence "
					+ sequenceBinary);
			// TODO: add aggregation of final values here...
		}
		// sort result
		// SortSplitter sortSplitter = new SortSplitter(this.directory,
		// this.outputDirectory.getName() + "-rev",
		// this.outputDirectory.getName(), this.indexName, this.statsName,
		// "", false);
		// sortSplitter.split(maxSequenceLength);
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

	private void initialize_absolteReaders(String continuationSequence,
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

	private void close_absoluteReaders() {
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
		File tempResultReverseSortDirectory = new File(
				this.lowTempResultDirectory.getAbsolutePath() + "-rev");
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
				tempResultReverseSortDirectory.getAbsolutePath() + "/"
						+ previousResultReverseSortSequence + "/"
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

	protected void initializeReverseSortResultWriter(String sequenceBinary,
			String currentFileName) {
		File currentReverseOutputDirectory = new File(
				this.outputDirectory.getAbsolutePath() + "-low-rev/"
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
				this.lowTempResultDirectory.getAbsolutePath() + "/"
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

	protected void initializeTempResultAbsoluteWriter(String sequenceBinary,
			String currentFileName) {
		File currentTempResultAbsoluteDirectory = new File(
				this.absTempResultDirectory.getAbsolutePath() + "/"
						+ sequenceBinary);
		currentTempResultAbsoluteDirectory.mkdir();
		this.tempResultWriter = IOHelper.openWriteFile(
				currentTempResultAbsoluteDirectory.getAbsolutePath() + "/"
						+ currentFileName + "." + sequenceBinary,
				Config.get().memoryLimitForWritingFiles);
	}

	private void closeAbsTempResultWriter() {
		try {
			this.tempResultWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void calculateDs(String directoryPath) {
		// TODO calculation
		long n1 = Counter.countCountsInDirectory(1, directoryPath);
		long n2 = Counter.countCountsInDirectory(2, directoryPath);
		System.out.println("n1: " + n1);
		System.out.println("n2: " + n2);
		// this.d1plus = 0.5;
		this.d1plus = n1 / ((double) n1 + 2 * n2);
		System.out.println(this.d1plus);
	}

	protected double getD(int _absoluteCount) {
		return this.d1plus;
	}

	protected double getDNumerator(int _absoluteCount,
			String _absoluteWordsWithoutLast) {
		return this.getD(_absoluteCount)
				* this.getAbsolute_Count(_absoluteWordsWithoutLast);
	}

	private double getAbsoluteWithoutLastCount(String absoluteWordsWithoutLast) {
		return this.absoluteWithoutLastReader
				.getCount(absoluteWordsWithoutLast);
	}

	private double getAbsolute_Count(String _absoluteWordsWithoutLast) {
		return this.absolute_Reader.getCount(_absoluteWordsWithoutLast);
	}

	private double get_absolute_Count(String _absoluteWordsWithoutLast,
			String current_absolute_Directory) {

		if (_absoluteWordsWithoutLast.isEmpty()) {
			return Counter.countColumnCountsInDirectory(0,
					current_absolute_Directory);
		} else {
			return this._absolute_Reader.getCount(_absoluteWordsWithoutLast);
		}
	}

	private String getRoundedResult(double input) {
		return this.decimalFormat.format(input);
	}
}
