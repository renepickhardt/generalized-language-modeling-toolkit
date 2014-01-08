package de.typology.smoother;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.typology.patterns.PatternTransformer;
import de.typology.utils.Config;
import de.typology.utils.Counter;
import de.typology.utils.DecimalFormatter;
import de.typology.utils.SequenceFormatter;

public class KneserNeySmoother {

	Logger logger = LogManager.getLogger(this.getClass().getName());

	// location of trained language models
	protected File absoluteDirectory;
	protected File continuationDirectory;

	// location of extracted language models which are needed for smoothing lm
	// for test data
	public File extractedAbsoluteDirectory;
	public File extractedContinuationDirectory;

	private String delimiter;
	private DecimalFormatter decimalFormatter;

	// in memory index of extracted counts for training data
	public HashMap<String, HashMap<String, Long>> absoluteTypeSequenceValueMap;
	public HashMap<String, HashMap<String, Long[]>> continuationTypeSequenceValueMap;
	protected HashMap<String, HashMap<String, Double>> discountTypeValuesMap;

	// global field needed to store discount values in different files for
	// modified and standard kneser ney
	protected File discountTypesValuesMapFile;

	// true if we smooth generalized language models
	private boolean smoothComplex;

	private long totalUnigramCount;

	// removed global config variable decimal places from Constructor. does that
	// make sense?
	public KneserNeySmoother(File extractedSequenceDirectory,
			File absoluteDirectory, File continuationDirectory, String delimiter) {
		this.absoluteDirectory = absoluteDirectory;
		this.continuationDirectory = continuationDirectory;
		this.extractedAbsoluteDirectory = new File(
				extractedSequenceDirectory.getAbsolutePath() + "/"
						+ absoluteDirectory.getName());
		this.extractedContinuationDirectory = new File(
				extractedSequenceDirectory.getAbsolutePath() + "/"
						+ continuationDirectory.getName());

		this.delimiter = delimiter;
		this.decimalFormatter = new DecimalFormatter(Config.get().decimalPlaces);

		this.discountTypesValuesMapFile = new File(this.absoluteDirectory
				.getParentFile().getAbsolutePath()
				+ "/discount-values-kneser-ney.ser");
		this.discountTypeValuesMap = null;

		this.totalUnigramCount = Counter.aggregateCountsInDirectory(new File(
				absoluteDirectory.getAbsolutePath() + "/1"));
		this.logger.info("total unigram count: " + this.totalUnigramCount);
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
			int sequenceLength, boolean smoothComplex,
			boolean conditionalProbabilityOnly, boolean backOffAbsolute) {
		this.smoothComplex = smoothComplex;
		this.logger
				.info("start calculating kneser-ney (or mod kneser ney) of length "
						+ sequenceLength);

		// calculate discount Values or read them from local file
		this.logger.info("calculate or read discount values");

		this.discountTypeValuesMap = this.calculateDiscountValues(
				this.absoluteDirectory, this.continuationDirectory);

		if (resultFile.exists()) {
			resultFile.delete();
		}

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
				double currentResult;
				if (conditionalProbabilityOnly) {
					currentResult = this.calculateConditionalProbability(
							sequence, sequenceLength, sequenceStringPattern,
							backOffAbsolute);
				} else {
					currentResult = this.calculateProbability(sequence,
							sequenceLength, sequenceStringPattern,
							backOffAbsolute);
				}
				resultWriter.write(sequence + this.delimiter
						+ this.decimalFormatter.getRoundedResult(currentResult)
						+ "\n");
			}
			inputSequenceReader.close();
			resultWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public HashMap<String, HashMap<String, Long>> readAbsoluteValuesIntoHashMap(
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
		// also add total count of 1grams
		HashMap<String, Long> aggregated1GramsMap = new HashMap<String, Long>();
		aggregated1GramsMap.put("", Counter
				.aggregateCountsInDirectory(new File(this.absoluteDirectory
						.getAbsolutePath() + "/1")));
		typeSequenceValueMap.put("", aggregated1GramsMap);
		return typeSequenceValueMap;

	}

	public HashMap<String, HashMap<String, Long[]>> readContinuationValuesIntoHashMap(
			File inputDirectory) {
		HashMap<String, HashMap<String, Long[]>> typeSequenceValueMap = new HashMap<String, HashMap<String, Long[]>>();

		for (File typeDirectory : inputDirectory.listFiles()) {
			HashMap<String, Long[]> sequenceValuesMap = new HashMap<String, Long[]>();
			for (File sequenceValueFile : typeDirectory.listFiles()) {
				try {
					BufferedReader sequenceValueReader = new BufferedReader(
							new FileReader(sequenceValueFile));
					String line;
					if (sequenceValueFile.getName().equals("all")) {
						while ((line = sequenceValueReader.readLine()) != null) {
							String[] lineSplit = line.split(this.delimiter);
							Long[] currentResultTypeArray = new Long[4];

							// [0]=1+
							// [1]=1
							// [2]=2
							// [3]=3+
							currentResultTypeArray[0] = Long
									.parseLong(lineSplit[0]);
							currentResultTypeArray[1] = Long
									.parseLong(lineSplit[1]);
							currentResultTypeArray[2] = Long
									.parseLong(lineSplit[2]);
							currentResultTypeArray[3] = Long
									.parseLong(lineSplit[3]);
							sequenceValuesMap.put("", currentResultTypeArray);
						}
					} else {
						while ((line = sequenceValueReader.readLine()) != null) {
							String[] lineSplit = line.split(this.delimiter);
							Long[] currentResultTypeArray = new Long[4];

							// [0]=1+
							// [1]=1
							// [2]=2
							// [3]=3+
							currentResultTypeArray[0] = Long
									.parseLong(lineSplit[1]);
							currentResultTypeArray[1] = Long
									.parseLong(lineSplit[2]);
							currentResultTypeArray[2] = Long
									.parseLong(lineSplit[3]);
							currentResultTypeArray[3] = Long
									.parseLong(lineSplit[4]);
							sequenceValuesMap.put(lineSplit[0],
									currentResultTypeArray);
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

	/**
	 * calculates a probability of a given sequence to be seen in a trained
	 * language model
	 * 
	 * The probability will be calculated as:
	 * 
	 * P(w1...w5) = P(w5|w1...w4) * P(w4|w1...w3) *...* P(w1)
	 * 
	 * later it could be possible to calculate the log of the sequence
	 * 
	 * @param sequence
	 * @param sequenceLength
	 * @param sequenceStringPattern
	 * @param backoffAbsolute
	 * @return
	 */
	protected double calculateProbability(String sequence, int sequenceLength,
			String sequenceStringPattern, boolean backoffAbsolute) {
		// double probability = 1;
		double logProbability = 0;
		String[] sequenceSplit = sequence.split("\\s");
		for (int i = 0; i < sequenceLength; i++) {
			String newSequence = "";
			int newSequenceLength = 0;
			String newSequenceStringPattern = "";
			for (int j = 0; j <= i; j++) {
				newSequence += sequenceSplit[j] + " ";
				newSequenceLength++;
				newSequenceStringPattern += "1";
			}
			newSequence = newSequence.replaceFirst(" $", "");
			double currentResult = this.calculateConditionalProbability(
					newSequence, newSequenceLength, newSequenceStringPattern,
					backoffAbsolute);
			if (currentResult <= 0) {
				this.logger.error("zero probability at: " + newSequence + " , "
						+ newSequenceLength + " , " + newSequenceStringPattern);
				currentResult = 0.000000000001;
			}
			logProbability += Math.log(currentResult) / Math.log(2.0);

		}
		return logProbability;
	}

	/**
	 * conditional probability of a given sequence w_{1}...w_{n} this will be
	 * evaluated as P(w_{n}|w_{1}...w_{n-1}). Is this true?
	 * 
	 * it will use smoothing with interpolation and backof in order to calculate
	 * the probabilities from the training data
	 * 
	 * @param sequence
	 * @param sequenceLength
	 * @param sequenceStringPattern
	 * @param backoffAbsolute
	 * @return
	 */
	protected double calculateConditionalProbability(String sequence,
			int sequenceLength, String sequenceStringPattern,
			boolean backoffAbsolute) {
		// calculate highest order result
		long highestOrderValue = this.getAbsoluteValue(sequenceStringPattern,
				sequence);
		if (sequenceLength == 1 && highestOrderValue == 0) {
			return (double) 1 / (this.totalUnigramCount + 1);
		}

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
		if (highestOrderDenominator == 0) {
			// calculate result of sequence without first word

			if (backoffAbsolute) {
				String sequenceWithoutFirst = SequenceFormatter.removeWord(
						sequence, 0);
				String sequenceStringPatternWithoutFirst = sequenceStringPattern
						.substring(1);
				this.logger.debug("zero denominator for: " + sequence
						+ " --> backoff to " + sequenceWithoutFirst);
				return this.calculateConditionalProbability(
						sequenceWithoutFirst, sequenceLength - 1,
						sequenceStringPatternWithoutFirst, backoffAbsolute);
			} else {
				this.logger.debug("zero denominator for: " + sequence);
				return this.calculateAggregatedLowerOrderResult(sequence,
						sequenceLength, sequenceStringPattern, backoffAbsolute);
			}

		}

		double result = highestOrderNumerator
				/ highestOrderDenominator
				+ this.calculateWeightNumerator(sequenceStringPattern,
						sequence, sequenceLength, sequenceStringPattern)
				/ highestOrderDenominator
				* this.calculateAggregatedLowerOrderResult(sequence,
						sequenceLength, sequenceStringPattern, backoffAbsolute);
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
				+ this.calculateWeightNumerator(sequenceStringPattern,
						sequence, sequenceLength, sequenceStringPattern) + "/"
				+ highestOrderDenominator + "*KNlowAggr("
				+ sequenceStringPattern + ")=" + result);
		return result;
	}

	protected double calculateAggregatedLowerOrderResult(
			String higherOrderSequence, int higherOrderSequenceLength,
			String higherOrderStringPattern, boolean backoffAbsolute) {
		if (higherOrderSequenceLength < 2) {
			return 0;
		}

		double aggregatedLowerOrderValue = 0;

		if (this.smoothComplex) {
			char[] higherOrderCharPattern = higherOrderStringPattern
					.toCharArray();
			this.logger.debug("for lower order (" + higherOrderStringPattern
					+ ") for \"" + higherOrderSequence + "\" aggregate:");
			// count skipped zeros to remove the correct word for the lower
			// order sequence
			int skippedZeros = 0;
			for (int i = 0; i < higherOrderCharPattern.length; i++) {
				if (higherOrderCharPattern[i] == '1') {
					char[] lowerOrderCharPattern = higherOrderCharPattern
							.clone();
					lowerOrderCharPattern[i] = '0';
					String lowerOrderSequence = null;
					// FIXME: conjecture: this leads to a huge perplexity gain
					// because also the last word in a sequence is being removed
					// see results of commit
					// faed2240527d573b16d2ae5e27e3dc5d1620a82e
					// if (i == 0) {
					// // remove all leading zeros
					// while (lowerOrderCharPattern[0] == '0'
					// && lowerOrderCharPattern.length > 1) {
					// lowerOrderCharPattern = Arrays.copyOfRange(
					// lowerOrderCharPattern, 1,
					// lowerOrderCharPattern.length);
					// }
					// } else {
					// if (i == higherOrderCharPattern.length - 1) {
					// // remove all zeros at the end of the pattern
					// while (lowerOrderCharPattern[lowerOrderCharPattern.length
					// - 1] == '0'
					// && lowerOrderCharPattern.length > 1) {
					// lowerOrderCharPattern = Arrays.copyOfRange(
					// lowerOrderCharPattern, 0,
					// lowerOrderCharPattern.length - 1);
					// }
					// }
					// }
					// lowerOrderSequence = SequenceFormatter.removeWord(
					// higherOrderSequence, i - skippedZeros);

					if (i == 0) {
						while (lowerOrderCharPattern[0] == '0'
								&& lowerOrderCharPattern.length > 1) {
							lowerOrderCharPattern = Arrays.copyOfRange(
									lowerOrderCharPattern, 1,
									lowerOrderCharPattern.length);
							lowerOrderSequence = SequenceFormatter.removeWord(
									higherOrderSequence, 0);
						}
					} else {
						lowerOrderSequence = SequenceFormatter.removeWord(
								higherOrderSequence, i - skippedZeros);
					}

					if (lowerOrderSequence.length() > 0) {
						String lowerOrderStringPattern = String
								.copyValueOf(lowerOrderCharPattern);
						double currentLowerOrderValue = this
								.calculateLowerOrderResult(lowerOrderSequence,
										higherOrderSequenceLength - 1,
										lowerOrderStringPattern,
										backoffAbsolute);
						aggregatedLowerOrderValue += currentLowerOrderValue;
					}
				} else {
					skippedZeros++;
				}

			}

			// FIXME: here in the last commit
			// (faed2240527d573b16d2ae5e27e3dc5d1620a82e) should have been
			// written double result = aggregatedLowerOrderValue /
			// higherOrderSequenceLength; instead but never mind
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
					higherOrderSequenceLength - 1, lowerOrderStringPattern,
					backoffAbsolute);
			this.logger.debug("lower order result ("
					+ +higherOrderSequenceLength + ") for \""
					+ higherOrderSequence + "\"=" + result);

			return result;
		}

	}

	protected double calculateLowerOrderResult(String sequence,
			int sequenceLength, String sequenceStringPattern,
			boolean backoffAbsolute) {
		String continuationPattern;
		if (sequenceStringPattern.contains("0")) {
			continuationPattern = sequenceStringPattern.replaceAll("0", "_");
		} else {
			continuationPattern = "_" + sequenceStringPattern;
		}
		long higherOrderValue = this.getContinuationValue(continuationPattern,
				sequence, 0);
		if (sequenceLength == 1 && higherOrderValue == 0) {
			return (double) 1 / (this.totalUnigramCount + 1);
		}

		double discountValue = this.getDiscountValue(continuationPattern,
				higherOrderValue);

		double highestOrderNumerator = higherOrderValue - discountValue;
		if (highestOrderNumerator < 0) {
			highestOrderNumerator = 0;
		}

		String sequenceWithoutLast = SequenceFormatter.removeWord(sequence,
				sequenceLength - 1);
		String continuationReplacedLastStringPattern = continuationPattern
				.substring(0, continuationPattern.length() - 1) + "_";
		this.logger.debug("calculateLowerOrder: " + sequence + "("
				+ sequenceStringPattern + ")" + "-->" + sequenceWithoutLast
				+ "(" + continuationReplacedLastStringPattern + "):");
		long higherOrderDenominator = this.getContinuationValue(
				continuationReplacedLastStringPattern, sequenceWithoutLast, 0);

		// // the higher
		// if (higherOrderDenominator == 0) {
		// System.out.println("(" + continuationReplacedLastStringPattern
		// + ")" + sequenceWithoutLast + " not found");
		// this.logger.error("lower order denominator is zero!");
		// System.exit(1);
		// }

		// call methods for lower order results
		if (higherOrderDenominator == 0) {
			if (sequenceLength == 1) {
				this.logger
						.error("denominator is zero at sequence length 1 which is not possible");
				System.exit(1);
			}
			// calculate result of sequence without first word

			if (backoffAbsolute) {
				String sequenceWithoutFirst = SequenceFormatter.removeWord(
						sequence, 0);
				while (PatternTransformer
						.getBooleanPattern(sequenceStringPattern)[1] == false) {
					sequenceStringPattern = 1 + sequenceStringPattern
							.substring(2);
				}
				String sequenceStringPatternWithoutFirst = sequenceStringPattern
						.substring(1);
				this.logger.debug("zero denominator for: " + sequence
						+ " --> backoff to " + sequenceWithoutFirst);
				return this.calculateConditionalProbability(
						sequenceWithoutFirst, sequenceLength - 1,
						sequenceStringPatternWithoutFirst, backoffAbsolute);
			} else {
				this.logger.debug("zero denominator for: " + sequence);
				return this.calculateAggregatedLowerOrderResult(sequence,
						sequenceLength, sequenceStringPattern, backoffAbsolute);
			}

		}

		// call methods for lower order results
		double result = highestOrderNumerator
				/ higherOrderDenominator
				+ this.calculateWeightNumerator(continuationPattern, sequence,
						sequenceLength, sequenceStringPattern)
				// + discountValue
				// * this.calculateContinuationLast(sequence, sequenceLength,
				// sequenceStringPattern)
				/ higherOrderDenominator
				* this.calculateAggregatedLowerOrderResult(sequence,
						sequenceLength, sequenceStringPattern, backoffAbsolute);

		this.logger.debug("\tKNlow("
				+ sequenceStringPattern
				+ "): "
				+ higherOrderValue
				+ "-"
				+ discountValue
				+ "/"
				+ higherOrderDenominator
				+ "+"
				+ this.calculateWeightNumerator(continuationPattern, sequence,
						sequenceLength, sequenceStringPattern) + "/"
				+ higherOrderDenominator + "*KNlowAggr("
				+ sequenceStringPattern + ")=" + result);
		return result;
	}

	protected double calculateWeightNumerator(String continuationPattern,
			String sequence, int sequenceLength, String sequenceStringPattern) {
		return this.getDiscountValue(continuationPattern, 1)
				* this.calculateContinuationLast(sequence, sequenceLength,
						sequenceStringPattern, 0);
	}

	/**
	 * Controller method to either calculate the discount values for a given
	 * language model and store the results in a serialized file or if that file
	 * exists retrieve those values from that file
	 * 
	 * @param absoluteDirectory
	 * @param continuationDirectory
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected HashMap<String, HashMap<String, Double>> calculateDiscountValues(
			File absoluteDirectory, File continuationDirectory) {

		// calculate discount Values
		HashMap<String, HashMap<String, Double>> discountTypeValuesMap = new HashMap<String, HashMap<String, Double>>();
		try {
			if (this.discountTypesValuesMapFile.exists()) {
				FileInputStream fis = new FileInputStream(
						this.discountTypesValuesMapFile);
				ObjectInputStream ois = new ObjectInputStream(fis);
				discountTypeValuesMap = (HashMap<String, HashMap<String, Double>>) ois
						.readObject();
				ois.close();
			} else {
				this.calculateDiscountValues(discountTypeValuesMap,
						absoluteDirectory);
				this.calculateDiscountValues(discountTypeValuesMap,
						continuationDirectory);

				FileOutputStream fos = new FileOutputStream(
						this.discountTypesValuesMapFile);
				ObjectOutputStream oos = new ObjectOutputStream(fos);
				oos.writeObject(discountTypeValuesMap);
				oos.close();
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return discountTypeValuesMap;

	}

	/**
	 * calculates the discount values for kneser ney smoothing of a language
	 * model with
	 * 
	 * @param discountTypeValuesMap
	 * @param inputDirectory
	 * @return
	 */
	protected HashMap<String, HashMap<String, Double>> calculateDiscountValues(
			HashMap<String, HashMap<String, Double>> discountTypeValuesMap,
			File inputDirectory) {
		// an absoluteTypeDirectory could be a file handle e.g. to
		// /inputpath/dataset/lang/absolut/11001
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
			System.exit(1);

		}
		if (this.absoluteTypeSequenceValueMap.get(pattern)
				.containsKey(sequence)) {
			return this.absoluteTypeSequenceValueMap.get(pattern).get(sequence);
		} else {
			return 0;
		}
	}

	protected long getContinuationValue(String pattern, String sequence,
			int countIndex) {
		if (!this.continuationTypeSequenceValueMap.containsKey(pattern)) {
			this.logger.error("Continuation pattern not found:" + pattern);
			System.exit(1);
		}
		if (this.continuationTypeSequenceValueMap.get(pattern).containsKey(
				sequence)) {
			return this.continuationTypeSequenceValueMap.get(pattern).get(
					sequence)[countIndex];
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
			int sequenceLength, String sequenceStringPattern, int countIndex) {
		String sequenceWithoutLast = SequenceFormatter.removeWord(sequence,
				sequenceLength - 1);
		sequenceStringPattern = sequenceStringPattern.replaceAll("0", "_");
		String continuationLastPattern = sequenceStringPattern.substring(0,
				sequenceStringPattern.length() - 1) + "_";
		long continuationLastValue = this.getContinuationValue(
				continuationLastPattern, sequenceWithoutLast, countIndex);
		this.logger.debug("\t\tcontinuationLast(" + sequenceStringPattern + "("
				+ sequenceLength + ")-->" + continuationLastPattern + ") for "
				+ sequence + "-->" + sequenceWithoutLast + ":"
				+ continuationLastValue);
		return continuationLastValue;
	}

}