package de.typology.smoother;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;

import org.apache.commons.io.FileUtils;

import de.typology.splitter.GLMCounter;
import de.typology.utils.Config;
import de.typology.utils.IOHelper;
import de.typology.utils.SystemHelper;

@Deprecated
public class _absoluteDiscountAggregator {

	/**
	 * This class provides a method that aggregates and calculates the number of
	 * discounted word types (for calculation lambda) and continuation counts
	 * based on a higher order glm.
	 * <p>
	 * 
	 * @param args
	 * 
	 * @author Martin Koerner
	 */
	public static void main(String[] args) {
		_absoluteDiscountAggregator dca = new _absoluteDiscountAggregator(
				Config.get().outputDirectory + Config.get().inputDataSet,
				"aggregate");
		dca.aggregate(5);
	}

	// e.g. 1111 for 4grams or 101 for typo 2 edges
	protected String binaryTargetFormat;

	// e.g. 11111 for 4grams or 1101 for typo 2 edges
	protected String binaryContinuationFormat;
	protected boolean continationFileExists;

	// e.g. 11111 for 4grams or 1011 for typo 2 edges
	protected String binaryDiscountFormat;
	protected boolean discountFileExists;

	// target string for which continuation and discount is currently calculated
	protected String currentTarget;

	// target count if used (only, when |target|==max modelLength)
	protected int currentTargetCount;

	// current continuation String (e.g. 1+1001)
	protected String nextContinuation;
	protected boolean hasNextContinuation;
	// number of different continuation Strings
	protected int currentContinuationCount;

	// current discount String (e.g. 1001+1)
	protected String nextDiscount;
	protected boolean hasNextDiscount;
	// number of different discount Strings
	protected int currentDiscountCount;

	// base directory of corpus data, e.g.: .../wiki/de/
	protected String directory;

	// output directory
	protected File tempOutputDirectory;

	// continuation reader
	protected BufferedReader continuationReader;
	// discount reader
	protected BufferedReader discountReader;

	// target writer
	protected BufferedWriter targetWriter;

	public _absoluteDiscountAggregator(String directory,
			String outputDirectoryName) {
		this.directory = directory;
		this.tempOutputDirectory = new File(directory + outputDirectoryName
				+ "-unsorted/");
		// delete old output directory
		try {
			FileUtils.deleteDirectory(this.tempOutputDirectory);
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.tempOutputDirectory.mkdir();
	}

	public void aggregate(int maxSequenceLength) {
		IOHelper.strongLog("aggregating continuation and discount values of "
				+ this.directory + " into " + this.tempOutputDirectory);
		IOHelper.strongLog("DELETE TEMP FILES IS: "
				+ Config.get().deleteTempFiles);

		// regular cases: |sequenceBinary|<maxSequenceLength
		// ContinuationSorter cs = new ContinuationSorter();
		for (int sequenceDecimal = 1; sequenceDecimal < Math.pow(2,
				maxSequenceLength - 1); sequenceDecimal++) {
			String sequenceBinary = Integer.toBinaryString(sequenceDecimal);
			this.aggregateFiles(sequenceBinary);
			// cs.sortSecondCloumnDirectory(this.outputDirectory +
			// sequenceBinary,
			// String sequenceBinaryMod = sequenceBinary.replaceFirst("1", "0");
			// this.aggregateFiles(sequenceBinaryMod);
			// // cs.sortSecondCloumnDirectory(this.outputDirectory
			// // + sequenceBinaryMod, "_split", "");
			// this.mergeSmallestType(this.tempOutputDirectory + "/"
			// + sequenceBinaryMod);
		}
		// merge and sort aggregate-unmerged
		_absoluteSplitter csp = new _absoluteSplitter(this.directory,
				"aggregate-unmerged", "aggregate", "index.txt", "", "",
				Config.get().deleteTempFiles);
		csp.split(maxSequenceLength - 1);
		// count absolute files
		GLMCounter glmc = new GLMCounter(this.directory, "absolute",
				"counts-absolute");
		glmc.countAbsolute(1);
		glmc = new GLMCounter(this.directory, "aggregate", "counts-aggregate");
		glmc.countAbsolute(2);
		// remove files that have been aggregated
		if (Config.get().deleteTempFiles) {
			for (int sequenceDecimal = 2; sequenceDecimal < Math.pow(2,
					maxSequenceLength - 1); sequenceDecimal++) {
				String sequenceBinary = Integer.toBinaryString(sequenceDecimal);
				// String sequenceBinaryMod = sequenceBinary
				// .replaceFirst("1", "0");
				if (Config.get().deleteTempFiles) {
					if (sequenceBinary.length() != 1) {
						try {
							FileUtils.deleteDirectory(new File(this.directory
									+ "absolute/" + sequenceBinary + "/"));
							//
							// if (Integer.bitCount(sequenceDecimal) != 1) {
							// FileUtils.deleteDirectory(new File(
							// this.directory + "absolute/"
							// + sequenceBinaryMod + "/"));
							// }
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
			IOHelper.strongLog("deleting continuation");
			try {
				FileUtils.deleteDirectory(new File(this.directory
						+ "continuation/"));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		// special case: |sequenceBinary|==maxSequenceLength
		// at the moment: do nothing, since there is nothing to aggregate

	}

	private void initialize(String binaryTargetFormat, String currentFileName) {

		File continuationFile = new File(this.directory + "continuation/"
				+ this.binaryContinuationFormat + "/" + currentFileName + "."
				+ this.binaryContinuationFormat);
		// handle case where continuationFile doesn't exist

		if (continuationFile.exists()) {
			this.continuationReader = IOHelper.openReadFile(continuationFile
					.getAbsolutePath());
			this.continationFileExists = true;
			this.hasNextContinuation = true;
		} else {
			this.currentContinuationCount = 0;
			this.hasNextContinuation = false;
			this.continationFileExists = false;
		}

		File discountFile = new File(this.directory + "absolute/"
				+ this.binaryDiscountFormat + "/" + currentFileName + "."
				+ this.binaryDiscountFormat);

		// handle case where discountFile doesn't exist
		if (discountFile.exists()) {
			this.discountReader = IOHelper.openReadFile(discountFile
					.getAbsolutePath());
			this.discountFileExists = true;
		} else {
			this.currentDiscountCount = 0;
			this.discountFileExists = false;
		}
		new File(this.tempOutputDirectory + "/" + binaryTargetFormat).mkdir();
		// open target writer
		this.targetWriter = IOHelper.openWriteFile(
				this.tempOutputDirectory.getAbsolutePath() + "/"
						+ binaryTargetFormat + "/" + currentFileName + "."
						+ binaryTargetFormat,
				Config.get().memoryLimitForWritingFiles);

	}

	private void reset() {
		try {
			this.targetWriter.close();
			if (this.continationFileExists) {
				this.continuationReader.close();
			}
			if (this.discountFileExists) {
				this.discountReader.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.currentContinuationCount = 0;
		this.currentDiscountCount = 0;
		this.currentTargetCount = 0;
	}

	protected void mergeSmallestType(String inputPath) {
		File inputFile = new File(inputPath);
		if (Integer.bitCount(Integer.parseInt(inputFile.getName(), 2)) == 0) {
			File[] files = inputFile.listFiles();

			String fileExtension = inputFile.getName();
			IOHelper.log("merge all " + fileExtension);

			SystemHelper
					.runUnixCommand("cat "
							+ inputPath
							+ "/*| awk 'BEGIN{OFS=\"\t\"}{a+=$1;b+=$2}END{print a,b}' > "
							+ inputPath + "/all." + fileExtension);
			for (File file : files) {
				if (!file.getName().equals("all." + fileExtension)) {
					file.delete();
				}
			}
		}
	}

	private void aggregateFiles(String sequenceBinary) {

		IOHelper.log("aggregating " + sequenceBinary);
		this.binaryTargetFormat = sequenceBinary;
		this.binaryContinuationFormat = "1" + this.binaryTargetFormat;
		this.binaryDiscountFormat = this.binaryTargetFormat + "1";

		// build the union of the continuation and discount directory
		File[] continuationFiles = new File(this.directory + "continuation/"
				+ this.binaryContinuationFormat).listFiles();
		File[] discountFiles = new File(this.directory + "absolute/"
				+ this.binaryDiscountFormat).listFiles();
		HashSet<String> targetFiles = new HashSet<String>();
		for (File file : continuationFiles) {
			String fileHead = file.getName().split("\\.")[0];
			if (!targetFiles.contains(fileHead)) {
				targetFiles.add(fileHead);
			}
		}
		for (File file : discountFiles) {
			String fileHead = file.getName().split("\\.")[0];
			if (!targetFiles.contains(fileHead)) {
				targetFiles.add(fileHead);
			}
		}

		for (String targetFile : targetFiles) {
			this.initialize(this.binaryTargetFormat, targetFile);

			// initialize currentContinuation, currentDiscount, and counts
			this.currentTarget = null;
			this.currentContinuationCount = 0;
			this.currentDiscountCount = 0;
			this.getNextContinuation();
			this.getNextDiscount();

			// go on until done with both files
			while (this.hasNextContinuation || this.hasNextDiscount) {
				// go on as long both files contain words
				while (true) {
					if (this.hasNextContinuation && this.hasNextDiscount) {
						if (this.currentTarget == null) {
							// set target as smallest value
							if (this.nextContinuation
									.compareTo(this.nextDiscount) < 0) {
								// continuation is smaller
								this.currentTarget = this.nextContinuation;
							} else {
								// discount is smaller or equal
								this.currentTarget = this.nextDiscount;
							}
						}

						if (this.nextContinuation.compareTo(this.currentTarget) == 0) {
							this.currentContinuationCount++;
							this.getNextContinuation();
							continue;
						}

						if (this.nextDiscount.compareTo(this.currentTarget) == 0) {
							this.currentDiscountCount++;
							this.getNextDiscount();
							continue;
						}
						// print target
						this.writeTarget();
					} else {
						break;
					}
				}

				if (this.hasNextContinuation) {
					if (this.currentTarget == null) {
						// set target
						this.currentTarget = this.nextContinuation;
					}
					if (this.nextContinuation.compareTo(this.currentTarget) == 0) {
						this.currentContinuationCount++;
						this.getNextContinuation();
					} else {
						// print target
						this.writeTarget();
					}
				}
				// only discount left
				if (this.hasNextDiscount) {
					if (this.currentTarget == null) {
						// set target
						this.currentTarget = this.nextDiscount;
					}
					if (this.nextDiscount.compareTo(this.currentTarget) == 0) {
						this.currentDiscountCount++;
						this.getNextDiscount();
					} else {
						// print target
						this.writeTarget();
					}
				}

				if (!this.hasNextContinuation && !this.hasNextDiscount) {
					this.writeTarget();
					this.reset();
				}
			}
		}
	}

	private void getNextContinuation() {
		try {
			if (!this.continationFileExists
					|| (this.nextContinuation = this.continuationReader
							.readLine()) == null) {
				this.hasNextContinuation = false;
			} else {
				String[] lineSplit = this.nextContinuation.split("\t");
				this.nextContinuation = "";
				// remove first word and count
				for (int i = 1; i < lineSplit.length - 1; i++) {
					this.nextContinuation += lineSplit[i] + "\t";
				}
				this.hasNextContinuation = true;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void getNextDiscount() {
		try {
			if (!this.discountFileExists
					|| (this.nextDiscount = this.discountReader.readLine()) == null) {
				this.hasNextDiscount = false;
			} else {
				String[] lineSplit = this.nextDiscount.split("\t");
				this.nextDiscount = "";
				// remove last word and count
				for (int i = 0; i < lineSplit.length - 2; i++) {
					this.nextDiscount += lineSplit[i] + "\t";
				}
				this.hasNextDiscount = true;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void writeTarget() {
		// TODO:add calculation & writing
		try {
			this.targetWriter.write(this.currentTarget
					+ this.currentContinuationCount + "\t"
					+ this.currentDiscountCount + "\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.currentContinuationCount = 0;
		this.currentDiscountCount = 0;
		this.currentTarget = null;
	}
}
