package de.typology.smoother;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;

import org.apache.commons.io.FileUtils;

import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public class DeltaContinuationAggregator {

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
		// TODO Auto-generated method stub

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
	protected String currentContinuation;
	// number of different continuation Strings
	protected int currentContinuationCount;

	// current delta String (e.g. 1001+1)
	protected String currentDelta;
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

	public DeltaContinuationAggregator(String directory,
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

		// regular cases: |sequenceBinary|<maxSequenceLength
		for (int sequenceDecimal = 1; sequenceDecimal < Math.pow(2,
				maxSequenceLength - 1); sequenceDecimal++) {
			// convert sequence type into binary representation
			String sequenceBinary = Integer.toBinaryString(sequenceDecimal);

			this.binaryTargetFormat = sequenceBinary;

			// build the union of the continuation and delta directory
			File[] continuationFiles = new File(this.directory
					+ "glm-continuation").listFiles();
			File[] deltaFiles = new File(this.directory + "glm-absolute")
					.listFiles();
			HashSet<File> filesUnion = new HashSet<File>();
			for (File file : continuationFiles) {
				if (!filesUnion.contains(file)) {
					filesUnion.add(file);
				}
			}
			for (File file : deltaFiles) {
				if (!filesUnion.contains(file)) {
					filesUnion.add(file);
				}
			}
			File[] targetFiles = filesUnion.toArray(new File[0]);
			for (File targetFile : targetFiles) {
				this.initialize(sequenceBinary, targetFile.getName());

				// initialize currentContinuation, currentDelta, and counts
				boolean hasNextContinuation = this.getNextContinuation();
				boolean hasNextDelta = this.getNextDelta();
				this.resetCounts();
				boolean wasEqualPreviously = false;
				// go on until done with both files
				while (hasNextContinuation || hasNextDelta) {
					// go on as long both files contain words
					while (hasNextContinuation && hasNextDelta) {
						if (this.currentContinuation == this.currentDelta) {
							this.currentContinuationCount++;
							this.currentDeltaCount++;
							hasNextContinuation = this.getNextContinuation();
							hasNextDelta = this.getNextDelta();
							wasEqualPreviously = true;
						}

						// currentContinuation<currentDelta --> write current
						// continuation as target
						if (this.currentContinuation
								.compareTo(this.currentDelta) < 0) {
							if (wasEqualPreviously) {
								// TODO:write continuation and delta
							} else {
								// TODO:write continuation and 0
							}

						}
					}
				}
				this.reset();
			}
		}
		// special case: |sequenceBinary|==maxSequenceLength
	}

	private void initialize(String binaryTargetFormat, String currentFileName) {
		this.binaryTargetFormat = binaryTargetFormat;
		this.binaryContinuationFormat = "1" + binaryTargetFormat;
		this.binaryDeltaFormat = binaryTargetFormat + "1";

		File continuationFile = new File(this.directory + "glm-continuation/"
				+ this.binaryContinuationFormat + "/" + currentFileName + "."
				+ this.binaryContinuationFormat);
		// handle case where continuationFile doesn't exist
		if (continuationFile.exists()) {
			this.continuationReader = IOHelper.openReadFile(this.directory
					+ "glm-continuation/" + this.binaryContinuationFormat + "/"
					+ currentFileName + "." + this.binaryContinuationFormat);
			this.continationFileExists = true;
		} else {
			this.currentContinuationCount = 0;
			this.continationFileExists = false;
		}

		File deltaFile = new File(this.directory + "glm-absolute/"
				+ this.binaryDeltaFormat + "/" + currentFileName + "."
				+ this.binaryDeltaFormat);

		// handle case where deltaFile doesn't exist
		if (deltaFile.exists()) {
			this.deltaReader = IOHelper.openReadFile(this.directory
					+ "glm-absolute/" + this.binaryDeltaFormat + "/"
					+ currentFileName + "." + this.binaryDeltaFormat);
			this.deltaFileExists = true;
		} else {
			this.currentDeltaCount = 0;
			this.deltaFileExists = false;
		}

		// open target writer
		this.targetWriter = IOHelper.openWriteFile(this.outputDirectory
				+ binaryTargetFormat + "/" + currentFileName,
				Config.get().memoryLimitForWritingFiles);
	}

	private void reset() {
		try {
			this.targetWriter.close();
			this.continuationReader.close();
			this.deltaReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// this.binaryTargetFormat = null;
		// this.binaryContinuationFormat = null;
		// this.binaryDeltaFormat = null;
		// this.currentContinuation = null;
		// this.currentContinuationCount = 0;
	}

	private void resetCounts() {
		this.currentContinuationCount = 0;
		this.currentDeltaCount = 0;
		this.currentTargetCount = 0;
	}

	private boolean getNextContinuation() {
		try {
			if ((this.currentContinuation = this.continuationReader.readLine()) == null) {
				return false;
			} else {
				return true;
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	private boolean getNextDelta() {
		try {
			if ((this.currentDelta = this.deltaReader.readLine()) == null) {
				return false;
			} else {
				return true;
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
}
