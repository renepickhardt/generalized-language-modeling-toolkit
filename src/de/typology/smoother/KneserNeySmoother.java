package de.typology.smoother;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.typology.indexes.WordIndex;
import de.typology.patterns.PatternTransformer;
import de.typology.utils.Counter;
import de.typology.utils.DecimalFormatter;
import de.typology.utils.SlidingWindowReader;

public class KneserNeySmoother {
	private double d1plus;

	Logger logger = LogManager.getLogger(this.getClass().getName());

	private File absoluteDirectory;
	private File _absoluteDirectory;
	private File _absolute_Directory;
	private File absolute_Directory;
	private File kneserNeyDirectory;
	private File kneserNeyLowDirectory;
	private File kneserNeyLowTempDirectory;
	private File kneserNeyLowSecondColumDirectory;
	private File kneserNeyHighDirectory;
	private File kneserNeyHighTempDirectory;
	private File kneserNeyHighSecondColumDirectory;
	private WordIndex wordIndex;
	private String delimiter;
	private DecimalFormatter decimalFormatter;
	private boolean deleteTempFiles;

	public KneserNeySmoother(File absoluteDirectory, File _absoluteDirectory,
			File _absolute_Directory, File absolute_Directory,
			File kneserNeyOutputDirectory, WordIndex wordIndex,
			String delimiter, int decimalPlaces, boolean deleteTempFiles) {
		this.absoluteDirectory = absoluteDirectory;
		this._absoluteDirectory = _absoluteDirectory;
		this._absolute_Directory = _absolute_Directory;
		this.absolute_Directory = absolute_Directory;
		this.kneserNeyDirectory = kneserNeyOutputDirectory;
		this.kneserNeyLowDirectory = new File(
				kneserNeyOutputDirectory.getAbsolutePath() + "/low");
		this.kneserNeyLowTempDirectory = new File(
				kneserNeyOutputDirectory.getAbsolutePath() + "/low-temp");
		this.kneserNeyLowSecondColumDirectory = new File(
				kneserNeyOutputDirectory.getAbsolutePath()
						+ "/low-second-column");
		this.kneserNeyHighDirectory = new File(
				kneserNeyOutputDirectory.getAbsolutePath() + "/high");
		this.kneserNeyHighTempDirectory = new File(
				kneserNeyOutputDirectory.getAbsolutePath() + "/high-temp");
		this.kneserNeyHighSecondColumDirectory = new File(
				kneserNeyOutputDirectory.getAbsolutePath()
						+ "/high-second-column");

		this.wordIndex = wordIndex;
		this.delimiter = delimiter;
		this.decimalFormatter = new DecimalFormatter(decimalPlaces);
		this.deleteTempFiles = deleteTempFiles;
	};

	public void deleteResults() {
		if (this.kneserNeyDirectory.exists()) {
			try {
				FileUtils.deleteDirectory(this.kneserNeyDirectory);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void makeDirectories() {
		this.kneserNeyDirectory.mkdirs();
		this.kneserNeyLowDirectory.mkdir();
		this.kneserNeyLowTempDirectory.mkdir();
		this.kneserNeyLowSecondColumDirectory.mkdir();
		this.kneserNeyHighDirectory.mkdir();
		this.kneserNeyHighTempDirectory.mkdir();
		this.kneserNeyHighSecondColumDirectory.mkdir();

	}

	public void smoothSimple(ArrayList<boolean[]> patterns) {
		this.makeDirectories();
	}

	public void smoothComplex(ArrayList<boolean[]> patterns) {
		this.makeDirectories();

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

			this.buildHigherOrder(pattern, currentBackoffPatterns);

		}

		// get backoff patterns for highest order pattern
		ArrayList<boolean[]> currentBackoffPatterns = new ArrayList<boolean[]>();
		boolean[] highestPattern = patterns.get(patterns.size() - 1);

		// remove the first bit completely for the first pattern
		boolean[] firstBackoffPattern = Arrays.copyOfRange(highestPattern, 1,
				highestPattern.length);
		currentBackoffPatterns.add(firstBackoffPattern);

		// leave out the first and last sequence bit
		for (int j = 1; j < highestPattern.length - 1; j++) {
			boolean[] backOffPattern = highestPattern.clone();
			if (backOffPattern[j]) {
				backOffPattern[j] = false;
				currentBackoffPatterns.add(backOffPattern);
			}
		}
		this.buildHighestOrder(highestPattern, currentBackoffPatterns);
	}

	/**
	 * build smoothed values for 1
	 */
	protected void buildLowestOrder() {
		File currentKneserNeyOutputDirectory = new File(
				this.kneserNeyLowDirectory.getAbsolutePath() + "/1");
		// return if already built
		if (currentKneserNeyOutputDirectory.exists()) {
			return;
		}
		this.logger.info("build "
				+ currentKneserNeyOutputDirectory.getAbsolutePath());
		currentKneserNeyOutputDirectory.mkdir();

		// read value of __
		File current_absolute_File = new File(
				this._absolute_Directory.getAbsolutePath() + "/__/all");
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

		// TODO add threads
		File current_absoluteDirectory = new File(
				this._absoluteDirectory.getAbsolutePath() + "/_1");
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
										+ current_absoluteFile.getName()));
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
	 * build smoothed values for all except the lowest and highest order
	 * patterns
	 */
	protected void buildHigherOrder(boolean[] currentPattern,
			ArrayList<boolean[]> backoffPatterns) {
		String currentStringPattern = PatternTransformer
				.getStringPattern(currentPattern);
		String current_absoluteStringPattern = "_"
				+ PatternTransformer.getStringPattern(currentPattern);
		String current_absolute_StringPattern = current_absoluteStringPattern
				.substring(0, current_absoluteStringPattern.length() - 1) + "_";
		String currentabsolute_StringPattern = current_absoluteStringPattern
				.substring(0, currentStringPattern.length() - 1) + "_";

		this.logger.debug("currentPattern: "
				+ PatternTransformer.getStringPattern(currentPattern));
		this.logger.debug("current_absolutePattern: "
				+ current_absoluteStringPattern);
		this.logger.debug("current_absolute_Pattern: "
				+ current_absolute_StringPattern);
		this.logger.debug("currentabsolute_Pattern: "
				+ currentabsolute_StringPattern);
		File currentKneserNeyOutputDirectory = new File(
				this.kneserNeyLowDirectory.getAbsolutePath() + "/"
						+ currentStringPattern);
		// return if already built
		if (currentKneserNeyOutputDirectory.exists()) {
			return;
		}
		this.logger.info("build "
				+ currentKneserNeyOutputDirectory.getAbsolutePath());
		currentKneserNeyOutputDirectory.mkdir();

		// calculate TempResultPair and store it in HashMap with high order
		// sequence

		// calculate higher order smoothed value

		File current_absoluteDirectory = new File(
				this._absoluteDirectory.getAbsolutePath() + "/"
						+ currentStringPattern);

		HashMap<String, ResultTriple> tempResultMap = new HashMap<String, ResultTriple>();
		for (File current_absoluteFile : current_absoluteDirectory.listFiles()) {
			try {
				BufferedReader current_absoluteFileReader = new BufferedReader(
						new FileReader(current_absoluteFile));
				// open readers for high order result and low order weight
				SlidingWindowReader current_absolute_FileReader = new SlidingWindowReader(
						new FileReader(
								this._absolute_Directory.getAbsolutePath()
										+ "/" + current_absolute_StringPattern
										+ "/" + current_absoluteFile.getName()));
				SlidingWindowReader currentAbsolute_FileReader = new SlidingWindowReader(
						new FileReader(
								this.absolute_Directory.getAbsolutePath() + "/"
										+ currentabsolute_StringPattern + "/"
										+ current_absoluteFile.getName()));

				String _absoluteLine;
				while ((_absoluteLine = current_absoluteFileReader.readLine()) != null) {
					ResultTriple resultTriple = new ResultTriple();
					String[] _absoluteLineSplit = _absoluteLine
							.split(this.delimiter);

					// calculate higher order result
					String _absolute_Line = current_absolute_FileReader
							.getLine(_absoluteLineSplit[0]);
					String[] _absolute_LineSplit = _absolute_Line
							.split(this.delimiter);

					int _absoluteValue = Integer
							.parseInt(_absoluteLineSplit[1]);
					double d = this.getD(_absoluteValue);
					int _absolute_Value = Integer
							.parseInt(_absolute_LineSplit[1]);
					double highOrderSmoothedDenominator = _absoluteValue - d;
					if (highOrderSmoothedDenominator < 0) {
						highOrderSmoothedDenominator = 0;
					}
					double smoothedValue = highOrderSmoothedDenominator
							/ _absolute_Value;
					resultTriple.setSmoothedValue(smoothedValue);

					// calculate backoff weight
					String absolute_Line = currentAbsolute_FileReader
							.getLine(_absoluteLineSplit[0]);
					String[] absolute_LineSplit = absolute_Line
							.split(this.delimiter);
					double backoffWeight = d
							* Integer.parseInt(absolute_LineSplit[1])
							/ _absolute_Value;
					resultTriple.setBackOffWeight(backoffWeight);

					tempResultMap.put(_absoluteLineSplit[0], resultTriple);
				}
				current_absoluteFileReader.close();
				current_absolute_FileReader.close();
				currentAbsolute_FileReader.close();

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		// prepare sequence to fit lower order backoff patterns

		// get lower order results

		// open readers for low order results

		// aggregate lower order results (arithmetic mean)

	}

	/**
	 * build smoothed values for the highest order patterns
	 */
	protected void buildHighestOrder(boolean[] cuttentPattern,
			ArrayList<boolean[]> backoffPatterns) {

	}

	protected void calculateDs(File directory) {
		long n1 = Counter.countCountsInDirectory(1, directory);
		long n2 = Counter.countCountsInDirectory(2, directory);
		this.logger.info("n1: " + n1);
		this.logger.info("n2: " + n2);
		// this.d1plus = 0.5;
		this.d1plus = n1 / ((double) n1 + 2 * n2);
		this.logger.info("D1+: " + this.d1plus);
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