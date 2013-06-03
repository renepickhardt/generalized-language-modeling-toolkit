package de.typology.smoother;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;

import org.apache.commons.io.FileUtils;

import de.typology.utils.Config;
import de.typology.utils.IOHelper;
import de.typology.utils.SystemHelper;

public class ContinuationDeltaAggregator {

	/**
	 * This class provides a method that aggregates and calculates the delta and
	 * continuation probability based on a higher order glm.
	 * <p>
	 * TODO: add extended Kneser-Ney smoothing at some point here
	 * 
	 * @param args
	 * 
	 * @author Martin Koerner
	 */
	public static void main(String[] args) {
		ContinuationDeltaAggregator dca = new ContinuationDeltaAggregator(
				Config.get().outputDirectory + Config.get().inputDataSet,
				"glm-aggregate");
		dca.aggregate(5);
	}

	// e.g. 1111 for 4grams or 101 for typo 2 edges
	protected String binaryTargetFormat;

	// e.g. 11111 for 4grams or 1101 for typo 2 edges
	protected String binaryContinuationFormat;
	protected boolean continationFileExists;

	// e.g. 11111 for 4grams or 1011 for typo 2 edges
	protected String binaryDeltaFormat;
	protected boolean deltaFileExists;

	// target string for which continuation and delta is currently calculated
	protected String currentTarget;

	// target count if used (only, when |target|==max modelLength)
	protected int currentTargetCount;

	// current continuation String (e.g. 1+1001)
	protected String nextContinuation;
	protected boolean hasNextContinuation;
	// number of different continuation Strings
	protected int currentContinuationCount;

	// current delta String (e.g. 1001+1)
	protected String nextDelta;
	protected boolean hasNextDelta;
	// number of different delta Strings
	protected int currentDeltaCount;

	// base directory of corpus data, e.g.: .../wiki/de/
	protected String directory;

	// output directory
	protected String outputDirectory;

	// continuation reader
	protected BufferedReader continuationReader;
	// delta reader
	protected BufferedReader deltaReader;

	// target writer
	protected BufferedWriter targetWriter;

	public ContinuationDeltaAggregator(String directory,
			String outputDirectoryName) {
		this.directory = directory;
		this.outputDirectory = directory + outputDirectoryName + "/";
		// delete old output directory
		File outputDirectoryFile = new File(this.outputDirectory);
		try {
			FileUtils.deleteDirectory(outputDirectoryFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		outputDirectoryFile.mkdir();
	}

	public void aggregate(int maxSequenceLength) {
		IOHelper.strongLog("aggregating continuation and delta values of "
				+ this.directory + " into " + this.outputDirectory);
		IOHelper.strongLog("DELETE TEMP FILES IS: "
				+ Config.get().deleteTempFiles);

		// regular cases: |sequenceBinary|<maxSequenceLength
		for (int sequenceDecimal = 1; sequenceDecimal < Math.pow(2,
				maxSequenceLength - 1); sequenceDecimal++) {
			String sequenceBinary = Integer.toBinaryString(sequenceDecimal);
			this.aggregateNMinusOne(sequenceBinary);
			String sequenceBinaryMod = sequenceBinary.replaceFirst("1", "0");
			this.aggregateNMinusOne(sequenceBinaryMod);
			this.mergeSmallestType(this.outputDirectory + sequenceBinaryMod);
			// remove files that have been aggregated
			if (Config.get().deleteTempFiles) {
				if (sequenceBinary.length() != 1) {
					try {
						FileUtils.deleteDirectory(new File(this.directory
								+ "glm-absolute/" + sequenceBinary + "/"));
						//
						if (Integer.bitCount(sequenceDecimal) != 1) {
							FileUtils
									.deleteDirectory(new File(this.directory
											+ "glm-absolute/"
											+ sequenceBinaryMod + "/"));
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		if (Config.get().deleteTempFiles) {
			IOHelper.strongLog("deleting glm-continuation");
			try {
				FileUtils.deleteDirectory(new File(this.directory
						+ "glm-continuation/"));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		// special case: |sequenceBinary|==maxSequenceLength
		// at the moment: do nothing, since there is nothing to aggregate

	}

	private void initialize(String binaryTargetFormat, String currentFileName) {

		File continuationFile = new File(this.directory + "glm-continuation/"
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

		File deltaFile = new File(this.directory + "glm-absolute/"
				+ this.binaryDeltaFormat + "/" + currentFileName + "."
				+ this.binaryDeltaFormat);

		// handle case where deltaFile doesn't exist
		if (deltaFile.exists()) {
			this.deltaReader = IOHelper.openReadFile(deltaFile
					.getAbsolutePath());
			this.deltaFileExists = true;
		} else {
			this.currentDeltaCount = 0;
			this.deltaFileExists = false;
		}
		new File(this.outputDirectory + binaryTargetFormat).mkdir();
		// open target writer
		this.targetWriter = IOHelper.openWriteFile(this.outputDirectory
				+ binaryTargetFormat + "/" + currentFileName + "."
				+ binaryTargetFormat, Config.get().memoryLimitForWritingFiles);

	}

	private void reset() {
		try {
			this.targetWriter.close();
			if (this.continationFileExists) {
				this.continuationReader.close();
			}
			if (this.deltaFileExists) {
				this.deltaReader.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.currentContinuationCount = 0;
		this.currentDeltaCount = 0;
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

	private void aggregateNMinusOne(String sequenceBinary) {

		IOHelper.log("aggregating " + sequenceBinary);
		this.binaryTargetFormat = sequenceBinary;
		this.binaryContinuationFormat = "1" + this.binaryTargetFormat;
		this.binaryDeltaFormat = this.binaryTargetFormat + "1";

		// build the union of the continuation and delta directory
		File[] continuationFiles = new File(this.directory
				+ "glm-continuation/" + this.binaryContinuationFormat)
				.listFiles();
		File[] deltaFiles = new File(this.directory + "glm-absolute/"
				+ this.binaryDeltaFormat).listFiles();
		HashSet<String> targetFiles = new HashSet<String>();
		for (File file : continuationFiles) {
			String fileHead = file.getName().split("\\.")[0];
			if (!targetFiles.contains(fileHead)) {
				targetFiles.add(fileHead);
			}
		}
		for (File file : deltaFiles) {
			String fileHead = file.getName().split("\\.")[0];
			if (!targetFiles.contains(fileHead)) {
				targetFiles.add(fileHead);
			}
		}

		for (String targetFile : targetFiles) {
			this.initialize(this.binaryTargetFormat, targetFile);

			// initialize currentContinuation, currentDelta, and counts
			this.currentTarget = null;
			this.currentContinuationCount = 0;
			this.currentDeltaCount = 0;
			this.getNextContinuation();
			this.getNextDelta();

			// go on until done with both files
			while (this.hasNextContinuation || this.hasNextDelta) {
				// go on as long both files contain words
				while (true) {
					if (this.hasNextContinuation && this.hasNextDelta) {
						if (this.currentTarget == null) {
							// set target as smallest value
							if (this.nextContinuation.compareTo(this.nextDelta) < 0) {
								// continuation is smaller
								this.currentTarget = this.nextContinuation;
							} else {
								// delta is smaller or equal
								this.currentTarget = this.nextDelta;
							}
						}

						if (this.nextContinuation.compareTo(this.currentTarget) == 0) {
							this.currentContinuationCount++;
							this.getNextContinuation();
							continue;
						}

						if (this.nextDelta.compareTo(this.currentTarget) == 0) {
							this.currentDeltaCount++;
							this.getNextDelta();
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
				// only delta left
				if (this.hasNextDelta) {
					if (this.currentTarget == null) {
						// set target
						this.currentTarget = this.nextDelta;
					}
					if (this.nextDelta.compareTo(this.currentTarget) == 0) {
						this.currentDeltaCount++;
						this.getNextDelta();
					} else {
						// print target
						this.writeTarget();
					}
				}

				if (!this.hasNextContinuation && !this.hasNextDelta) {
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

	private void getNextDelta() {
		try {
			if (!this.deltaFileExists
					|| (this.nextDelta = this.deltaReader.readLine()) == null) {
				this.hasNextDelta = false;
			} else {
				String[] lineSplit = this.nextDelta.split("\t");
				this.nextDelta = "";
				// remove last word and count
				for (int i = 0; i < lineSplit.length - 2; i++) {
					this.nextDelta += lineSplit[i] + "\t";
				}
				this.hasNextDelta = true;
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
					+ this.currentDeltaCount + "\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.currentContinuationCount = 0;
		this.currentDeltaCount = 0;
		this.currentTarget = null;
	}
}
