package de.typology.smoother;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.typology.utils.Counter;
import de.typology.utils.DecimalFormatter;

public class KneserNeySmoother {
	private double d1plus;

	static Logger logger = LogManager.getLogger(KneserNeySmoother.class
			.getName());

	private File absoluteDirectory;
	private File _absoluteDirectory;
	private File _absolute_Directory;
	private File absolute_Directory;
	private File kneserNeyOutputDirectory;
	private String delimiter;
	private DecimalFormatter decimalFormatter;

	public KneserNeySmoother(File absoluteDirectory, File _absoluteDirectory,
			File _absolute_Directory, File absolute_Directory,
			File kneserNeyOutputDirectory, String delimiter, int decimalPlaces) {
		this.absoluteDirectory = absoluteDirectory;
		this._absoluteDirectory = _absoluteDirectory;
		this._absolute_Directory = _absolute_Directory;
		this.absolute_Directory = absolute_Directory;
		this.kneserNeyOutputDirectory = kneserNeyOutputDirectory;

		this.delimiter = delimiter;
		this.decimalFormatter = new DecimalFormatter(decimalPlaces);

		if (this.kneserNeyOutputDirectory.exists()) {
			try {
				FileUtils.deleteDirectory(this.kneserNeyOutputDirectory);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		this.kneserNeyOutputDirectory.mkdir();
	};

	public void smoothSimple(ArrayList<boolean[]> patterns) {
	}

	public void smoothComplex(ArrayList<boolean[]> patterns) {
		this.buildLowestOrder();
		// start at 1 to leave out lowest order and size-1 to leave out highest
		// order
		for (int i = 1; i < patterns.size() - 1; i++) {
			ArrayList<boolean[]> currentBackoffPatterns = new ArrayList<boolean[]>();
			boolean[] pattern = patterns.get(i);

			// remove the first bit completely for the first pattern
			boolean[] firstBackoffPattern = Arrays.copyOfRange(pattern, 1,
					pattern.length);
			currentBackoffPatterns.add(firstBackoffPattern);

			// leave out the first and last sequence bit
			for (int j = 1; j < pattern.length - 1; j++) {
				boolean[] backOffPattern = pattern.clone();
				if (backOffPattern[j]) {
					backOffPattern[j] = false;
					currentBackoffPatterns.add(backOffPattern);
				}
			}

			this.buildHigherOrder(currentBackoffPatterns);

		}

		// this.buildHighestOrder();
	}

	/**
	 * build smoothed values for 1
	 */
	protected void buildLowestOrder() {
		File currentKneserNeyOutputDirectory = new File(
				this.kneserNeyOutputDirectory.getAbsolutePath() + "/1");

		// read value of __
		File current_absolute_File = new File(
				this._absolute_Directory.getAbsolutePath() + "__/all");
		long __Value;
		BufferedReader current_absolute_FileReader;
		try {
			current_absolute_FileReader = new BufferedReader(new FileReader(
					current_absolute_File));
			__Value = Long.parseLong(current_absolute_FileReader.readLine());
			current_absolute_FileReader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}

		File current_absoluteDirectory = new File(
				this._absoluteDirectory.getAbsolutePath() + "/1");
		// read _1 files
		for (File current_absoluteFile : current_absoluteDirectory.listFiles()) {
			try {
				BufferedReader current_absoluteFileReader = new BufferedReader(
						new FileReader(current_absoluteFile));
				BufferedWriter currentResultWriter = new BufferedWriter(
						new FileWriter(
								currentKneserNeyOutputDirectory
										.getAbsolutePath()
										+ "/"
										+ current_absolute_File.getName()));
				String line;
				while ((line = current_absoluteFileReader.readLine()) != null) {
					String[] lineSplit = line.split(this.delimiter);
					// calculate lowest order result
					double currentResult = (double) Integer
							.parseInt(lineSplit[1]) / __Value;
					// write current result
					currentResultWriter.write(lineSplit[0]
							+ this.delimiter
							+ this.decimalFormatter
									.getRoundedResult(currentResult) + "\n");
				}
				current_absoluteFileReader.close();
				currentResultWriter.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * build smoothed values for all except the highest order patterns
	 */
	protected void buildHigherOrder(ArrayList<boolean[]> backoffPatterns) {

	}

	/**
	 * build smoothed values for the highest order patterns
	 */
	protected void buildHighestOrder(ArrayList<boolean[]> backoffPatterns) {

	}

	protected void calculateDs(File directory) {
		long n1 = Counter.countCountsInDirectory(1, directory);
		long n2 = Counter.countCountsInDirectory(2, directory);
		logger.info("n1: " + n1);
		logger.info("n2: " + n2);
		// this.d1plus = 0.5;
		this.d1plus = n1 / ((double) n1 + 2 * n2);
		logger.info("D1+: " + this.d1plus);
	}

	protected double getD(int _absoluteCount) {
		return this.d1plus;
	}

	// protected double getDNumerator(int _absoluteCount,
	// String _absoluteWordsWithoutLast) {
	// return this.getD(_absoluteCount)
	// * this.getAbsolute_Count(_absoluteWordsWithoutLast);
	// }

}