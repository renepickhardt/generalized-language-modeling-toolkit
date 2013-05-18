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
				this.currentTarget = null;
				this.currentContinuationCount = 0;
				this.currentDeltaCount = 0;
				this.getNextContinuation();
				this.getNextDelta();
				boolean progressOnContinuation = true;
				boolean progressOnDelta = true;

				// go on until done with both files
				while (this.hasNextContinuation || this.hasNextDelta) {
					// go on as long both files contain words
					while (this.hasNextContinuation && this.hasNextDelta) {
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
							progressOnContinuation = true;

						} else {
							progressOnContinuation = false;
						}

						if (this.nextDelta.compareTo(this.currentTarget) == 0) {
							this.currentDeltaCount++;
							this.getNextDelta();
							progressOnDelta = true;
						} else {
							progressOnDelta = false;
						}

						if (progressOnContinuation == false
								&& progressOnDelta == false) {
							// print target
							if (this.nextContinuation
									.compareTo(this.currentTarget) == 0
									&& this.nextDelta
											.compareTo(this.currentTarget) == 0) {
								this.writeContinuationAndDelta();
							} else {
								if (this.nextContinuation
										.compareTo(this.currentTarget) == 0) {
									this.writeContinuation();
								} else {
									if (this.nextDelta
											.compareTo(this.currentTarget) == 0) {
										this.writeDelta();
									} else {
										throw new IllegalStateException();
									}
								}
							}
						}

					}
					// only continuation left
					if (this.hasNextContinuation) {
						if (this.currentTarget == null) {
							// set target
							this.currentTarget = this.nextContinuation;
						}
						if (this.nextContinuation.compareTo(this.currentTarget) == 0) {
							this.currentContinuationCount++;
							this.getNextContinuation();
							progressOnContinuation = true;

						} else {
							progressOnContinuation = false;
						}
						if (progressOnContinuation == false) {
							// print target
							this.writeContinuation();
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
							progressOnDelta = true;

						} else {
							progressOnDelta = false;
						}
						if (progressOnDelta == false) {
							// print target
							this.writeDelta();
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
		this.currentContinuationCount = 0;
		this.currentDeltaCount = 0;
		this.currentTargetCount = 0;
	}

	private void getNextContinuation() {
		try {
			if ((this.nextContinuation = this.continuationReader.readLine()) == null) {
				this.hasNextContinuation = false;
			} else {
				// TODO: remove first word
				this.hasNextContinuation = true;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void getNextDelta() {
		try {
			if ((this.nextDelta = this.deltaReader.readLine()) == null) {
				this.hasNextDelta = false;
			} else {
				// TODO: remove last word (don't forget the count)
				this.hasNextDelta = true;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void writeContinuationAndDelta() {
		// TODO:add calculation & writing
		this.currentContinuationCount = 0;
		this.currentDeltaCount = 0;
		this.currentTarget = null;
	}

	private void writeContinuation() {
		// TODO:add calculation & writing
		this.currentContinuationCount = 0;
		this.currentTarget = null;
	}

	private void writeDelta() {
		// TODO:add calculation & writing
		this.currentDeltaCount = 0;
		this.currentTarget = null;
	}

}
