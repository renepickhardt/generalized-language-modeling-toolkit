package de.typology.smoother;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.typology.patterns.PatternTransformer;
import de.typology.utils.Counter;
import de.typology.utils.DecimalFormatter;
import de.typology.utils.SequenceFormatter;

public class KneserNeySmoother {

	Logger logger = LogManager.getLogger(this.getClass().getName());

	private File absoluteDirectory;
	private File extractedAbsoluteDirectory;
	private File extractedContinuationDirectory;

	private String delimiter;
	private DecimalFormatter decimalFormatter;
	protected HashMap<String, HashMap<String, Long>> absoluteTypeSequenceValueMap;
	protected HashMap<String, HashMap<String, Long>> continuationTypeSequenceValueMap;
	protected HashMap<String, HashMap<String, Double>> discountTypeValuesMap;

	private boolean smoothComplex;

	public KneserNeySmoother(File extractedSequenceDirectory,
			File absoluteDirectory, File continuationDirectory,
			String delimiter, int decimalPlaces) {
		this.absoluteDirectory = absoluteDirectory;
		this.extractedAbsoluteDirectory = new File(
				extractedSequenceDirectory.getAbsolutePath() + "/"
						+ absoluteDirectory.getName());
		this.extractedContinuationDirectory = new File(
				extractedSequenceDirectory.getAbsolutePath() + "/"
						+ continuationDirectory.getName());

		// calculate discount Values
		this.discountTypeValuesMap = this.calculateDiscountValues(
				this.absoluteDirectory, continuationDirectory);

		this.delimiter = delimiter;
		this.decimalFormatter = new DecimalFormatter(decimalPlaces);
	};

	/**
	 * if smoothComplex==false: calculate traditional Kneser-Ney smoothing based
	 * on Chen and Goodman
	 * 
	 * @param inputSequenceFile
	 * @param resultFile
	 * @param smoothComplex
	 * @param maxModelLength
	 * @param cores
	 */
	public void smooth(File inputSequenceFile, File resultFile,
			int sequenceLength, boolean smoothComplex) {
		this.smoothComplex = smoothComplex;
		if (resultFile.exists()) {
			resultFile.delete();
		}

		// read absolute and continuation values into HashMaps
		this.absoluteTypeSequenceValueMap = this
				.readValuesIntoHashMap(this.extractedAbsoluteDirectory);

		// also add total count of 1grams
		HashMap<String, Long> aggregated1GramsMap = new HashMap<String, Long>();
		aggregated1GramsMap.put("", Counter
				.aggregateCountsInDirectory(new File(this.absoluteDirectory
						.getAbsolutePath() + "/1")));
		this.absoluteTypeSequenceValueMap.put("", aggregated1GramsMap);

		this.continuationTypeSequenceValueMap = this
				.readValuesIntoHashMap(this.extractedContinuationDirectory);

		// go through sequence file
		try {
			BufferedReader inputSequenceReader = new BufferedReader(
					new FileReader(inputSequenceFile));
			BufferedWriter resultWriter = new BufferedWriter(new FileWriter(
					resultFile));
			String sequence;
			String sequenceStringPattern = PatternTransformer
					.getStringPattern(PatternTransformer
							.getBooleanPatternWithOnes(sequenceLength));
			while ((sequence = inputSequenceReader.readLine()) != null) {
				resultWriter.write(sequence
						+ this.delimiter
						+ this.decimalFormatter.getRoundedResult(this
								.calculateResult(sequence, sequenceLength,
										sequenceStringPattern)) + "\n");
			}
			inputSequenceReader.close();
			resultWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private HashMap<String, HashMap<String, Long>> readValuesIntoHashMap(
			File inputDirectory) {
		HashMap<String, HashMap<String, Long>> typeSequenceValueMap = new HashMap<String, HashMap<String, Long>>();
		for (File typeDirectory : inputDirectory.listFiles()) {
			HashMap<String, Long> sequenceValuesMap = new HashMap<String, Long>();
			for (File sequenceValueFile : typeDirectory.listFiles()) {
				try {
					BufferedReader sequenceValueReader = new BufferedReader(
							new FileReader(sequenceValueFile));
					String line;
					if (sequenceValueFile.getName().equals("all")) {
						while ((line = sequenceValueReader.readLine()) != null) {
							sequenceValuesMap.put("", Long.parseLong(line));
						}
					} else {
						while ((line = sequenceValueReader.readLine()) != null) {
							String[] lineSplit = line.split(this.delimiter);
							sequenceValuesMap.put(lineSplit[0],
									Long.parseLong(lineSplit[1]));
						}
					}
					sequenceValueReader.close();
				} catch (NumberFormatException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			typeSequenceValueMap
					.put(typeDirectory.getName(), sequenceValuesMap);

		}
		return typeSequenceValueMap;

	}

	protected double calculateResult(String sequence, int sequenceLength,
			String sequenceStringPattern) {
		this.logger.debug("calculate(" + sequenceStringPattern + "):"
				+ sequence);
		// calculate highest order result
		long highestOrderValue = this.getAbsoluteValue(sequenceStringPattern,
				sequence);
		double discountValue = this.getDiscountValue(sequenceStringPattern,
				highestOrderValue);

		double highestOrderNumerator = highestOrderValue - discountValue;
		if (highestOrderNumerator < 0) {
			highestOrderNumerator = 0;
		}
		String sequenceWithoutLast = SequenceFormatter.removeWord(sequence,
				sequenceLength - 1);
		String sequencePatternWithoutLast = sequenceStringPattern.substring(0,
				sequenceLength - 1);
		long highestOrderDenominator = this.getAbsoluteValue(
				sequencePatternWithoutLast, sequenceWithoutLast);

		// call methods for lower order results
		// TODO what if highestOrderDenominator==0?
		if (highestOrderDenominator == 0) {
			return 0;
		}

		double result = highestOrderNumerator
				/ highestOrderDenominator
				+ discountValue
				* this.calculateContinuationLast(sequence, sequenceLength,
						sequenceStringPattern)
				/ highestOrderDenominator
				* this.calculateAggregatedLowerOrderResult(sequence,
						sequenceStringPattern, sequenceLength);
		this.logger.debug("KNhigh("
				+ sequenceStringPattern
				+ "): "
				+ highestOrderValue
				+ "-"
				+ discountValue
				+ "/"
				+ highestOrderDenominator
				+ "+"
				+ discountValue
				+ "*"
				+ this.calculateContinuationLast(sequence, sequenceLength,
						sequenceStringPattern) + "/" + highestOrderDenominator
				+ "*KNlow(" + sequenceStringPattern + ")=" + result);
		return result;
	}

	private double calculateAggregatedLowerOrderResult(
			String higherOrderSequence, String higherOrderStringPattern,
			int higherOrderSequenceLength) {
		if (higherOrderSequenceLength < 2) {
			return 0;
		}

		double aggregatedLowerOrderValue = 0;

		if (this.smoothComplex) {
			char[] higherOrderCharPattern = higherOrderStringPattern
					.toCharArray();
			this.logger.debug("for lower order (" + higherOrderStringPattern
					+ ") for \"" + higherOrderSequence + "\" aggregate:");
			for (int i = 0; i < higherOrderSequenceLength - 1; i++) {
				if (higherOrderCharPattern[i] == '1') {
					char[] lowerOrderCharPattern = higherOrderCharPattern
							.clone();
					lowerOrderCharPattern[i] = '0';
					String lowerOrderSequence = null;
					if (i == 0) {
						while (lowerOrderCharPattern[0] == '0') {
							lowerOrderCharPattern = Arrays.copyOfRange(
									lowerOrderCharPattern, 1,
									lowerOrderCharPattern.length);
							lowerOrderSequence = SequenceFormatter.removeWord(
									higherOrderSequence, 0);
						}
					} else {
						lowerOrderSequence = SequenceFormatter.removeWord(
								higherOrderSequence, i);
					}

					String lowerOrderStringPattern = String
							.copyValueOf(lowerOrderCharPattern);
					double currentLowerOrderValue = this
							.calculateLowerOrderResult(lowerOrderSequence,
									higherOrderSequenceLength,
									lowerOrderStringPattern);
					this.logger.debug("    (" + lowerOrderStringPattern
							+ "): \"" + lowerOrderSequence + "\"");
					aggregatedLowerOrderValue += currentLowerOrderValue;
				}

			}

			double result = aggregatedLowerOrderValue
					/ (higherOrderSequenceLength - 1);
			this.logger.debug("lower order result (" + higherOrderStringPattern
					+ ") for \"" + higherOrderSequence + "\":"
					+ aggregatedLowerOrderValue + "/"
					+ (higherOrderSequenceLength - 1) + "=" + result);
			return result;
		} else {
			String lowerOrderSequence = SequenceFormatter.removeWord(
					higherOrderSequence, 0);
			String lowerOrderStringPattern = higherOrderStringPattern
					.substring(1);

			double result = this.calculateLowerOrderResult(lowerOrderSequence,
					higherOrderSequenceLength - 1, lowerOrderStringPattern);
			this.logger.debug("lower order result ("
					+ +higherOrderSequenceLength + ") for \""
					+ higherOrderSequence + "\"=" + result);

			return result;
		}

	}

	protected double calculateLowerOrderResult(String sequence,
			int sequenceLength, String sequenceStringPattern) {
		String continuationPattern;
		if (sequenceStringPattern.contains("0")) {
			continuationPattern = sequenceStringPattern.replaceAll("0", "_");
		} else {
			continuationPattern = "_" + sequenceStringPattern;
		}
		long higherOrderValue = this.getContinuationValue(continuationPattern,
				sequence);

		double discountValue = this.getDiscountValue(continuationPattern,
				higherOrderValue);

		double highestOrderNumerator = higherOrderValue - discountValue;
		if (highestOrderNumerator < 0) {
			highestOrderNumerator = 0;
		}

		String sequenceWithoutLast = SequenceFormatter.removeWord(sequence,
				sequenceLength - 1);
		String continuationReplacedLastStringPattern = "_"
				+ sequenceStringPattern.substring(0, sequenceLength - 1) + "_";
		long higherOrderDenominator = this.getContinuationValue(
				continuationReplacedLastStringPattern, sequenceWithoutLast);

		// TODO: change this
		// if (higherOrderDenominator == 0) {
		// System.out.println("(" + continuationReplacedLastStringPattern
		// + ")" + sequenceWithoutLast + " not found");
		// return 0;
		// }

		// call methods for lower order results
		double result = highestOrderNumerator
				/ higherOrderDenominator
				+ discountValue
				* this.calculateContinuationLast(sequence, sequenceLength,
						sequenceStringPattern)
				/ higherOrderDenominator
				* this.calculateAggregatedLowerOrderResult(sequence,
						sequenceStringPattern, sequenceLength);

		this.logger.debug("KNlow("
				+ sequenceStringPattern
				+ "): "
				+ higherOrderValue
				+ "-"
				+ discountValue
				+ "/"
				+ higherOrderDenominator
				+ "+"
				+ discountValue
				+ "*"
				+ this.calculateContinuationLast(sequence, sequenceLength,
						sequenceStringPattern) + "/" + higherOrderDenominator
				+ "*KNlow(" + sequenceStringPattern + ")=" + result);
		return result;
	}

	protected HashMap<String, HashMap<String, Double>> calculateDiscountValues(
			File absoluteDirectory, File continuationDirectory) {
		HashMap<String, HashMap<String, Double>> discountTypeValuesMap = new HashMap<String, HashMap<String, Double>>();
		this.calculateDiscountValues(discountTypeValuesMap, absoluteDirectory);
		this.calculateDiscountValues(discountTypeValuesMap,
				continuationDirectory);

		return discountTypeValuesMap;

	}

	protected HashMap<String, HashMap<String, Double>> calculateDiscountValues(
			HashMap<String, HashMap<String, Double>> discountTypeValuesMap,
			File inputDirectory) {
		for (File absoluteTypeDirectory : inputDirectory.listFiles()) {
			if (absoluteTypeDirectory.getName().contains("split")) {
				continue;
			}
			HashMap<String, Double> discountValuesMap = new HashMap<String, Double>();
			long n1 = Counter.countCountsInDirectory(1, absoluteTypeDirectory,
					"<fs>");
			long n2 = Counter.countCountsInDirectory(2, absoluteTypeDirectory,
					"<fs>");
			this.logger.info("n1 for " + absoluteTypeDirectory.getName() + ":"
					+ n1);
			this.logger.info("n2 for " + absoluteTypeDirectory.getName() + ":"
					+ n2);
			// this.d1plus = 0.5;
			double d1plus = n1 / ((double) n1 + 2 * n2);
			this.logger.info("D1+ for " + absoluteTypeDirectory.getName() + ":"
					+ d1plus);
			discountValuesMap.put("D1+", d1plus);

			discountTypeValuesMap.put(absoluteTypeDirectory.getName(),
					discountValuesMap);
		}
		return discountTypeValuesMap;

	}

	protected long getAbsoluteValue(String pattern, String sequence) {
		if (!this.absoluteTypeSequenceValueMap.containsKey(pattern)) {
			this.logger.error("Absolute pattern not found:" + pattern);

			try {
				throw new FileNotFoundException(
						"could not find absolute pattern: " + pattern);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}

			return 0;
		}
		if (this.absoluteTypeSequenceValueMap.get(pattern)
				.containsKey(sequence)) {
			return this.absoluteTypeSequenceValueMap.get(pattern).get(sequence);
		} else {
			return 0;
		}
	}

	protected long getContinuationValue(String pattern, String sequence) {
		if (!this.continuationTypeSequenceValueMap.containsKey(pattern)) {
			this.logger.error("Continuation pattern not found:" + pattern);
			try {
				throw new FileNotFoundException(
						"could not find continuation pattern: " + pattern);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			return 0;
		}
		if (this.continuationTypeSequenceValueMap.get(pattern).containsKey(
				sequence)) {
			return this.continuationTypeSequenceValueMap.get(pattern).get(
					sequence);
		} else {
			return 0;
		}
	}

	// the following methods are overwritten by ModifiedKneserNeySmoother

	/**
	 * sequenceCount is not not needed here. Yet, it is needed for modified
	 * Kneser-Ney
	 * 
	 * @param sequenceStringPattern
	 * @param sequenceCount
	 * @return
	 */
	protected double getDiscountValue(String sequenceStringPattern,
			long sequenceCount) {
		String stringPatternForBitcount = sequenceStringPattern.replaceAll("_",
				"0");
		if (Integer.bitCount(PatternTransformer
				.getIntPattern(PatternTransformer
						.getBooleanPattern(stringPatternForBitcount))) > 1) {
			return this.discountTypeValuesMap.get(sequenceStringPattern).get(
					"D1+");
		} else {
			return 0;
		}
	}

	protected double calculateContinuationLast(String sequence,
			int sequenceLength, String sequenceStringPattern) {

		String sequenceWithoutLast = SequenceFormatter.removeWord(sequence,
				sequenceLength - 1);
		String continuationLastPattern = sequenceStringPattern.substring(0,
				sequenceStringPattern.length() - 1) + "_";
		long continuationLastValue = this.getContinuationValue(
				continuationLastPattern, sequenceWithoutLast);
		this.logger.debug("continuationLast(" + sequenceStringPattern
				+ ") for " + sequence + ":" + continuationLastValue);
		return continuationLastValue;
	}

}