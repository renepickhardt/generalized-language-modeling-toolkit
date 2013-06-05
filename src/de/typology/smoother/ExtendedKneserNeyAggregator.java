package de.typology.smoother;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

public class ExtendedKneserNeyAggregator {
	private double d1;
	private double d2;
	private double d3plus;
	private String aggregatedFile;

	// base directory of corpus data, e.g.: .../wiki/de/
	protected String directory;

	// directories for absolute and aggregate glms
	protected String absoluteDirectory;
	protected String aggregateDirectory;

	// output directory
	protected String outputDirectory;

	// previous file reader
	protected BufferedReader aggregateReader;

	// result writer
	protected BufferedWriter targetWriter;

	public ExtendedKneserNeyAggregator(String directory,
			String outputDirectoryName) {
		this.directory = directory;
		this.absoluteDirectory = directory + "absolute/";
		this.aggregateDirectory = directory + "aggregate/";
		this.outputDirectory = directory + outputDirectoryName + "/";
		this.calculateDs();
		// delete old output directory
		File outputDirectoryFile = new File(this.outputDirectory);
		try {
			FileUtils.deleteDirectory(outputDirectoryFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		outputDirectoryFile.mkdir();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	private void initialize(String binaryTargetFormat, String currentFileName) {

	}

	private void calculateDs() {
		// TODO calculation
		this.d1 = 0.5;
		this.d2 = this.d1;
		this.d3plus = this.d2;
	}

	/**
	 * 
	 * @param maxSequenceLength
	 *            needs to be greater than 1
	 */
	private void calculate(int maxSequenceLength) {

		// case: 1
		int sequenceDecimal = 1;
		String sequenceBinary = Integer.toBinaryString(sequenceDecimal);

		// case: 2 .. max-1
		for (sequenceDecimal = 2; sequenceDecimal < Math.pow(2,
				maxSequenceLength - 1); sequenceDecimal++) {
			sequenceBinary = Integer.toBinaryString(sequenceDecimal);

		}

		// case: max
		for (sequenceDecimal = (int) Math.pow(2, maxSequenceLength - 1); sequenceDecimal < Math
				.pow(2, maxSequenceLength); sequenceDecimal++) {
			sequenceBinary = Integer.toBinaryString(sequenceDecimal);

		}
	}
}
