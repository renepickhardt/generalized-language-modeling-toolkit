package de.typology.smootherOld;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import de.typology.utils.Config;
import de.typology.utilsOld.Counter;
import de.typology.utilsOld.IOHelper;
import de.typology.utilsOld.SystemHelper;

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

	protected File directory;
	protected String outputDirectoryName;
	protected File absoluteDirectory;
	protected File _absoluteDirectory;
	protected File absolute_Directory;
	protected File _absolute_Directory;
	protected File outputDirectory;
	protected File lowDiscountValueTempDirectory;
	protected File lowDiscountValueDirectory;
	protected File highDiscountValueTempDirectory;
	protected File highDiscountValueDirectory;
	protected File lowTempResultDirectory;
	protected File highTempResultDirectory;
	protected File lowTempReverseSortDirectory;
	protected File highTempReverseSortDirectory;

	protected BufferedReader absoluteReader;
	protected BufferedReader _absoluteReader;
	protected SlidingWindowReader absolute_Reader;
	protected SlidingWindowReader _absolute_Reader;
	protected SlidingWindowReader absoluteWithoutLastReader;

	protected BufferedWriter tempResultWriter;
	protected BufferedWriter discountValueWriter;

	private String indexName;
	private String statsName;

	private double d1plus;

	private KneserNeyFormatter kneserNeyFormatter;

	public KneserNeyAggregator(String directory, String absoluteDirectoryName,
			String _absoluteDirectoryName, String absolute_DirectoryName,
			String _absolute_DirectoryName, String outputDirectoryName,
			String indexName, String statsName) {
		this.indexName = indexName;
		this.statsName = statsName;
		this.directory = new File(directory);
		this.outputDirectoryName = outputDirectoryName;
		this.outputDirectory = new File(this.directory.getAbsolutePath() + "/"
				+ outputDirectoryName);
		this.absoluteDirectory = new File(this.directory.getAbsolutePath()
				+ "/" + absoluteDirectoryName);
		this._absoluteDirectory = new File(this.directory.getAbsolutePath()
				+ "/" + _absoluteDirectoryName);
		this.absolute_Directory = new File(this.directory.getAbsolutePath()
				+ "/" + absolute_DirectoryName);
		this._absolute_Directory = new File(this.directory.getAbsolutePath()
				+ "/" + _absolute_DirectoryName);
		this.lowDiscountValueTempDirectory = new File(
				this.directory.getAbsolutePath() + "/" + outputDirectoryName
						+ "-low-discount-temp");
		this.lowDiscountValueDirectory = new File(
				this.directory.getAbsolutePath() + "/" + outputDirectoryName
						+ "-low-discount");
		this.highDiscountValueTempDirectory = new File(
				this.directory.getAbsolutePath() + "/" + outputDirectoryName
						+ "-high-discount-temp");
		this.highDiscountValueDirectory = new File(
				this.directory.getAbsolutePath() + "/" + outputDirectoryName
						+ "-high-discount");
		this.lowTempResultDirectory = new File(this.directory.getAbsolutePath()
				+ "/" + outputDirectoryName + "-low-temp");
		this.highTempResultDirectory = new File(
				this.directory.getAbsolutePath() + "/" + outputDirectoryName
						+ "-high-temp");
		this.lowTempReverseSortDirectory = new File(
				this.directory.getAbsolutePath() + "/" + outputDirectoryName
						+ "-low-temp-rev");
		this.highTempReverseSortDirectory = new File(
				this.directory.getAbsolutePath() + "/" + outputDirectoryName
						+ "-high-temp-rev");

		IOHelper.strongLog("delete old output folders");
		try {
			for (File file : this.directory.listFiles()) {
				if (file.getName().startsWith(outputDirectoryName)) {
					FileUtils.deleteDirectory(file);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.lowDiscountValueTempDirectory.mkdir();
		this.lowDiscountValueDirectory.mkdir();
		this.highDiscountValueTempDirectory.mkdir();
		this.highDiscountValueDirectory.mkdir();
		this.lowTempResultDirectory.mkdir();
		this.highTempResultDirectory.mkdir();
		this.lowTempReverseSortDirectory.mkdir();
		this.highTempReverseSortDirectory.mkdir();
		this.kneserNeyFormatter = new KneserNeyFormatter();
	}

	/**
	 * 
	 * @param maxSequenceLength
	 *            needs to be greater than 1
	 */
	protected void calculate(int maxSequenceLength) {
		IOHelper.strongLog("calcualting kneser-ney weights for "
				+ this.directory);
		long startTime = System.currentTimeMillis();

		this.calculateTempResults(maxSequenceLength);

		KneserNeyTempResultCombiner resultCombiner = new KneserNeyTempResultCombiner(
				this.directory, this.outputDirectory, this.indexName,
				this.statsName);
		resultCombiner.combine("-low", "-low", "-temp", "-rev",
				maxSequenceLength - 1, false);
		if (Config.get().resultLog10) {
			resultCombiner.combine("-high", "-low", "-temp", "-rev",
					maxSequenceLength, true);
		} else {
			resultCombiner.combine("-high", "-low", "-temp", "-rev",
					maxSequenceLength, false);
		}

		// merge smallest sequence results
		this.mergeSmallestType(this.lowTempResultDirectory.getAbsolutePath()
				.replace("-temp", ""));
		this.mergeSmallestType(this.lowDiscountValueDirectory.getAbsolutePath());
		this.mergeSmallestType(this.highDiscountValueDirectory
				.getAbsolutePath());
		try {
			// delete high 1 result (because it's wrong...)
			FileUtils.deleteDirectory(new File(this.highTempResultDirectory
					.getAbsolutePath().replace("-temp", "") + "/1"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// delete temp files
		// TODO delete smarter (earlier)
		if (Config.get().deleteTempFiles) {
			for (File file : this.directory.listFiles()) {
				if (file.getName().startsWith(this.outputDirectoryName)
						&& (file.getName().contains("-temp") || file.getName()
								.contains("rev"))) {
					try {
						FileUtils.deleteDirectory(file);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		long endTime = System.currentTimeMillis();
		long time = (endTime - startTime) / 1000;
		IOHelper.strongLog("time for calculating kneser-ney of "
				+ this.directory.getAbsolutePath() + ": " + time + "s");
	}

	private void calculateTempResults(int maxSequenceLength) {
		for (int sequenceDecimal = 1; sequenceDecimal < Math.pow(2,
				maxSequenceLength); sequenceDecimal++) {
			String sequenceBinary = Integer.toBinaryString(sequenceDecimal);
			// skip even results (since there is no target)
			if (sequenceDecimal % 2 == 0) {
				continue;
			}
			// build absolute results
			if (sequenceDecimal > 1) {
				IOHelper.strongLog("calculating absolute sequence "
						+ sequenceBinary);
				String currentAbsolteDirectory = this.absoluteDirectory + "/"
						+ sequenceBinary;
				this.calculateDs(currentAbsolteDirectory);
				System.out.println(currentAbsolteDirectory);
				for (File absoluteFile : new File(currentAbsolteDirectory)
						.listFiles()) {
					String absoluteFileName = absoluteFile.getName().split(
							"\\.")[0];
					this.initializeAbsoluteReaders(sequenceBinary,
							absoluteFileName);
					this.initializeAbsolute_Reader(sequenceBinary,
							absoluteFileName);
					this.initializeTempDiscountValueWriter(sequenceBinary,
							absoluteFileName, false);
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

								// calculate the discount value
								double discountFractionResult = this
										.getDNumerator(absoluteCount,
												absoluteWordsWithoutLast)
										/ absoluteWithoutLastCount;
								this.discountValueWriter
										.write(absoluteWordsWithoutLast
												+ this.kneserNeyFormatter
														.getRoundedResult(discountFractionResult)
												+ "\n");
								// System.out.println(absoluteWords + ": "
								// + absoluteCount + "-"
								// + this.getD(absoluteCount) + "/"
								// + absoluteWithoutLastCount);
								this.tempResultWriter
										.write(absoluteWords
												+ this.kneserNeyFormatter
														.getRoundedResult(firstFractionResult)
												+ "\t"
												+ this.kneserNeyFormatter
														.getRoundedResult(discountFractionResult)
												+ "\n");
							}
						} finally {
							this.closeAbsoluteReaders();
							this.closeAbsolute_Reader();
							this.closeTempDiscountValueWriter();
							this.closeAbsTempResultWriter();
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
					// aggregate absolute discount fraction values...
					String sequenceBinaryWithoutLast = sequenceBinary
							.substring(0, sequenceBinary.length() - 1);
					String filePathPartTwo = sequenceBinaryWithoutLast + "/"
							+ absoluteFileName + "."
							+ sequenceBinaryWithoutLast;
					File tempDiscountValueFile = new File(
							this.highDiscountValueTempDirectory
									.getAbsolutePath() + "/" + filePathPartTwo);
					File discountValueFile = new File(
							this.highDiscountValueDirectory.getAbsolutePath()
									+ "/" + filePathPartTwo);
					this.aggregateDiscountValues(tempDiscountValueFile,
							discountValueFile);
				}
			}
			// sequenceLength<maxLength
			if (sequenceBinary.length() < maxSequenceLength) {
				IOHelper.strongLog("calculating lower order sequence "
						+ sequenceBinary);
				String current_absolteDirectory = this._absoluteDirectory
						+ "/_" + sequenceBinary;
				this.calculateDs(current_absolteDirectory);
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
						this.initializeTempDiscountValueWriter(sequenceBinary,
								_absoluteFileName, true);
					}
					this.initializeLowResultWriter(sequenceBinary,
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
													+ this.kneserNeyFormatter
															.getRoundedResult(kneserNeyResult)
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
									this.discountValueWriter
											.write(_absoluteWordsWithoutLast
													+ this.kneserNeyFormatter
															.getRoundedResult(discountFractionResult)
													+ "\n");
									// System.out.println(_absoluteWords + ": "
									// + continuationMinusDResult + " / "
									// + _absolute_Count);
									this.tempResultWriter
											.write(_absoluteWords
													+ this.kneserNeyFormatter
															.getRoundedResult(firstFractionResult)
													+ "\t"
													+ this.kneserNeyFormatter
															.getRoundedResult(discountFractionResult)
													+ "\n");
								}
							}
						} finally {
							this.close_absoluteReaders();
							if (sequenceDecimal > 1) {
								this.closeAbsolute_Reader();
								this.closeTempDiscountValueWriter();
							}
							this.closeLowResultWriter();
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
					if (sequenceDecimal > 1) {
						// aggregate absolute discount fraction values...
						String sequenceBinaryWithoutLast = sequenceBinary
								.substring(0, sequenceBinary.length() - 1);
						String filePathPartTwo = sequenceBinaryWithoutLast
								+ "/" + _absoluteFileName + "."
								+ sequenceBinaryWithoutLast;
						File tempDiscountValueFile = new File(
								this.lowDiscountValueTempDirectory
										.getAbsolutePath()
										+ "/"
										+ filePathPartTwo);
						File discountValueFile = new File(
								this.lowDiscountValueDirectory
										.getAbsolutePath()
										+ "/"
										+ filePathPartTwo);
						this.aggregateDiscountValues(tempDiscountValueFile,
								discountValueFile);
					}
				}
			}
		}
	}

	private void aggregateDiscountValues(File inputFile, File outputFile) {
		BufferedReader tempDiscountValueReader = IOHelper.openReadFile(
				inputFile.getAbsolutePath(),
				Config.get().memoryLimitForReadingFiles);

		outputFile.getParentFile().mkdirs();
		BufferedWriter discountValueWriter = IOHelper.openWriteFile(
				outputFile.getAbsolutePath(),
				Config.get().memoryLimitForWritingFiles);
		String currentLine;
		String previousLine = "";
		try {
			try {
				while ((currentLine = tempDiscountValueReader.readLine()) != null) {
					if (!currentLine.equals(previousLine)) {
						discountValueWriter.write(currentLine + "\n");
						previousLine = currentLine;
					}
				}
			} finally {
				tempDiscountValueReader.close();
				discountValueWriter.close();
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		if (Config.get().deleteTempFiles) {
			inputFile.delete();
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

	protected void initializeLowResultWriter(String sequenceBinary,
			String currentFileName) {
		File currentTempResultDirectory;
		currentTempResultDirectory = new File(
				this.lowTempResultDirectory.getAbsolutePath() + "/"
						+ sequenceBinary);
		currentTempResultDirectory.mkdir();
		this.tempResultWriter = IOHelper.openWriteFile(
				currentTempResultDirectory.getAbsolutePath() + "/"
						+ currentFileName + "." + sequenceBinary,
				Config.get().memoryLimitForWritingFiles);
	}

	private void closeLowResultWriter() {
		try {
			this.tempResultWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void initializeTempDiscountValueWriter(String sequenceBinary,
			String currentFileName, boolean low) {
		String sequenceBinaryWithoutLast = sequenceBinary.substring(0,
				sequenceBinary.length() - 1);
		File currentDiscountValueDirectory;
		if (low) {
			currentDiscountValueDirectory = new File(
					this.lowDiscountValueTempDirectory.getAbsolutePath() + "/"
							+ sequenceBinaryWithoutLast);
		} else {
			currentDiscountValueDirectory = new File(
					this.highDiscountValueTempDirectory.getAbsolutePath() + "/"
							+ sequenceBinaryWithoutLast);
		}
		currentDiscountValueDirectory.mkdir();
		this.discountValueWriter = IOHelper.openWriteFile(
				currentDiscountValueDirectory.getAbsolutePath() + "/"
						+ currentFileName + "." + sequenceBinaryWithoutLast,
				Config.get().memoryLimitForWritingFiles);
	}

	private void closeTempDiscountValueWriter() {
		try {
			this.discountValueWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void initializeTempResultAbsoluteWriter(String sequenceBinary,
			String currentFileName) {
		File currentTempResultAbsoluteDirectory = new File(
				this.highTempResultDirectory.getAbsolutePath() + "/"
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
		long n1 = Counter.countCountsInDirectory(1, directoryPath);
		long n2 = Counter.countCountsInDirectory(2, directoryPath);
		IOHelper.log("n1: " + n1);
		IOHelper.log("n2: " + n2);
		// this.d1plus = 0.5;
		this.d1plus = n1 / ((double) n1 + 2 * n2);
		IOHelper.log("D1+: " + this.d1plus);
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

	protected double getAbsolute_Count(String _absoluteWordsWithoutLast) {
		return this.absolute_Reader.getCount(_absoluteWordsWithoutLast);
	}

	protected double getAbsolute_Count(String _absoluteWordsWithoutLast,
			int columnStartZero) {
		return this.absolute_Reader.getCount(_absoluteWordsWithoutLast,
				columnStartZero);
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

	protected void mergeSmallestType(String inputPath) {
		for (File subDirectory : new File(inputPath).listFiles()) {
			if (Integer.bitCount(Integer.parseInt(subDirectory.getName(), 2)) == 1) {
				File[] files = subDirectory.listFiles();
				String fileExtension = subDirectory.getName();
				IOHelper.log("merge all " + fileExtension);
				SystemHelper.runUnixCommand("cat "
						+ subDirectory.getAbsolutePath() + "/* > "
						+ subDirectory.getAbsolutePath() + "/all."
						+ fileExtension);
				for (File file : files) {
					if (!file.getName().equals("all." + fileExtension)) {
						file.delete();
					}
				}
			}
		}
	}

}
