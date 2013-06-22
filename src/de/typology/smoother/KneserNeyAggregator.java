package de.typology.smoother;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

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
				"absolute-rev-sort", "continuation-rev-sort", "ns-rev-sort",
				"kneser-ney");
		kna.calculate(5);

	}

	private double d1plus;
	protected BufferedReader continuationReader;
	protected BufferedReader nreader;
	protected BufferedReader absoluteReader;
	protected BufferedWriter writer;

	// buffers the current type of continuations for calculating the denominator
	// N_{1+}(*w_{i+1}^{n-1}*)
	protected HashMap<String, Integer> continuationBuffer;
	protected String directory;
	protected File continuationDirectory;
	protected File nDirectory;
	protected File outputDirectory;

	public KneserNeyAggregator(String directory, String absoluteDirectoryName,
			String continuationDirectoryName, String nDirectoryName,
			String outputDirectoryName) {
		this.directory = directory;
		this.continuationDirectory = new File(this.directory
				+ continuationDirectoryName);
		this.nDirectory = new File(this.directory + nDirectoryName);
		this.outputDirectory = new File(this.directory + outputDirectoryName);
		if (this.outputDirectory.exists()) {
			try {
				FileUtils.deleteDirectory(this.outputDirectory);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		this.outputDirectory.mkdir();
	}

	private void calculateDs() {
		// TODO calculation
		this.d1plus = 0.5;
	}

	private void initialize(String binaryTargetFormat, String currentFileName) {

	}

	/**
	 * 
	 * @param maxSequenceLength
	 *            needs to be greater than 1
	 */
	private void calculate(int maxSequenceLength) {
		IOHelper.strongLog("calcualting kneser-ney weights for "
				+ this.directory);

		// // lowest level: length=1
		// int sequenceDecimal = 1;
		// String sequenceBinary = Integer.toBinaryString(sequenceDecimal);
		// // divide continuation count from 1 by summed continuation count
		// // write result at new file folder /kneser-ney (append at row of old
		// // file)
		// String continuationSequenceBinary = "_" + sequenceBinary;
		// String currentContinuationPath = this.continuationDirectory
		// .getAbsolutePath() + "/" + continuationSequenceBinary;
		// String currentNPath = this.nDirectory.getAbsolutePath() + "/__/";
		// File currentOutputDirectory = new File(
		// this.outputDirectory.getAbsolutePath() + "/" + sequenceBinary);
		// currentOutputDirectory.mkdir();
		//
		// long continuationSum = Counter.countColumnCountsInDirectory(0,
		// currentNPath);
		// IOHelper.strongLog("calculating " + sequenceBinary);
		// System.out.println("continuationSum " + continuationSum);
		// System.out.println("continuationPath: " + currentContinuationPath);
		// for (File continuationFile : new File(currentContinuationPath)
		// .listFiles()) {
		// String fileName = continuationFile.getName().split("\\.")[0];
		// this.continuationReader = IOHelper.openReadFile(
		// continuationFile.getAbsolutePath(),
		// Config.get().memoryLimitForReadingFiles);
		// this.writer = IOHelper.openWriteFile(currentOutputDirectory
		// .getAbsolutePath() + "/" + fileName + "." + sequenceBinary);
		// String continuationLine;
		// String[] continuationLineSplit;
		// try {
		// try {
		// while ((continuationLine = this.continuationReader
		// .readLine()) != null) {
		// continuationLineSplit = continuationLine.split("\t");
		// double continuationResult = Double
		// .parseDouble(continuationLineSplit[1])
		// / continuationSum;
		// this.writer.write(continuationLineSplit[0] + "\t"
		// + continuationResult + "\n");
		// }
		// } finally {
		// this.continuationReader.close();
		// this.writer.close();
		// }
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// }

		for (int sequenceDecimal = 1; sequenceDecimal < Math.pow(2,
				maxSequenceLength - 1); sequenceDecimal++) {
			String sequenceBinary = Integer.toBinaryString(sequenceDecimal);
			// skip even results (since there is no target)
			if (sequenceDecimal % 2 == 0) {
				continue;
			}
			String continuationSequenceBinary = "_" + sequenceBinary;
			String currentContinuationPath = this.continuationDirectory
					.getAbsolutePath() + "/" + continuationSequenceBinary;
			String currentNDenominator = continuationSequenceBinary.substring(
					0, continuationSequenceBinary.length() - 1) + "_";
			System.out.println("currentNDenominator: " + currentNDenominator);
			String currentNPath = this.nDirectory.getAbsolutePath() + "/"
					+ currentNDenominator;
			File currentOutputDirectory = new File(
					this.outputDirectory.getAbsolutePath() + "/"
							+ sequenceBinary);
			currentOutputDirectory.mkdir();

			// -1 since e.g. 11 --> _11 --> _1_
			long continuationSum = Counter.countColumnCountsInDirectory(
					Integer.bitCount(Integer.parseInt(sequenceBinary, 2)) - 1,
					currentNPath);

			IOHelper.strongLog("calculating " + sequenceBinary);
			System.out.println("continuationSum " + continuationSum);
			System.out.println("continuationPath: " + currentContinuationPath);
			for (File continuationFile : new File(currentContinuationPath)
					.listFiles()) {
				String fileName = continuationFile.getName().split("\\.")[0];
				this.continuationReader = IOHelper.openReadFile(
						continuationFile.getAbsolutePath(),
						Config.get().memoryLimitForReadingFiles);
				this.writer = IOHelper.openWriteFile(currentOutputDirectory
						.getAbsolutePath()
						+ "/"
						+ fileName
						+ "."
						+ sequenceBinary);
				String continuationLine;
				String[] continuationLineSplit;
				try {
					try {
						while ((continuationLine = this.continuationReader
								.readLine()) != null) {
							continuationLineSplit = continuationLine
									.split("\t");
							double kneserNeyResult;
							if (sequenceDecimal == 1) {
								double continuationResult = Double
										.parseDouble(continuationLineSplit[Integer
												.bitCount(Integer.parseInt(
														sequenceBinary, 2))])
										/ continuationSum;
								kneserNeyResult = continuationResult;
							} else {
								kneserNeyResult = 0;
							}
							for (int i = 0; i < continuationLineSplit.length - 1; i++) {
								this.writer.write(continuationLineSplit[i]
										+ "\t");
							}
							this.writer.write(kneserNeyResult + "\n");
						}
					} finally {
						this.continuationReader.close();
						this.writer.close();
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}
		//
		// // length=modelLength
		// for (sequenceDecimal = (int) Math.pow(2, maxSequenceLength - 1);
		// sequenceDecimal < Math
		// .pow(2, maxSequenceLength); sequenceDecimal++) {
		// sequenceBinary = Integer.toBinaryString(sequenceDecimal);
		//
		// }
	}
}
