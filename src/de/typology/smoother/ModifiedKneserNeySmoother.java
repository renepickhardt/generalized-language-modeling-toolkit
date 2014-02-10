package de.typology.smoother;

import java.io.File;
import java.util.HashMap;

import de.typology.patterns.PatternTransformer;
import de.typology.utils.Counter;

public class ModifiedKneserNeySmoother extends KneserNeySmoother {

	public ModifiedKneserNeySmoother(File extractedSequenceDirectory,
			File absoluteDirectory, File continuationDirectory,
			String delimiter, int decimalPlaces) {
		super(extractedSequenceDirectory, absoluteDirectory,
				continuationDirectory, delimiter);

		this.discountTypesValuesMapFile = new File(this.absoluteDirectory
				.getParentFile().getAbsolutePath()
				+ "/discount-values-mod-kneser-ney.ser");

	}

	private double d1;
	private double d2;
	private double d3plus;

	/**
	 * @param args
	 */

	@Override
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
			long n3 = Counter.countCountsInDirectory(3, absoluteTypeDirectory,
					"<fs>");
			long n4 = Counter.countCountsInDirectory(4, absoluteTypeDirectory,
					"<fs>");
			this.logger.info("n1 for " + absoluteTypeDirectory.getName() + ":"
					+ n1);
			this.logger.info("n2 for " + absoluteTypeDirectory.getName() + ":"
					+ n2);
			this.logger.info("n3 for " + absoluteTypeDirectory.getName() + ":"
					+ n3);
			this.logger.info("n4 for " + absoluteTypeDirectory.getName() + ":"
					+ n4);
			double y = n1 / ((double) n1 + 2 * n2);
			this.d1 = 1 - 2 * y * ((double) n2 / (double) n1);
			this.d2 = 2 - 3 * y * ((double) n3 / (double) n2);
			this.d3plus = 3 - 4 * y * ((double) n4 / (double) n3);
			// this.d1plus = 0.5;
			this.logger.info("D1 for " + absoluteTypeDirectory.getName() + ":"
					+ this.d1);
			this.logger.info("D2 for " + absoluteTypeDirectory.getName() + ":"
					+ this.d2);
			this.logger.info("D3+ for " + absoluteTypeDirectory.getName() + ":"
					+ this.d3plus);
			discountValuesMap.put("D1", this.d1);
			discountValuesMap.put("D2", this.d2);
			discountValuesMap.put("D3+", this.d3plus);

			discountTypeValuesMap.put(absoluteTypeDirectory.getName(),
					discountValuesMap);
		}
		return discountTypeValuesMap;

	}

	/**
	 * 
	 * @param sequenceStringPattern
	 * @param sequenceCount
	 * @return
	 */
	@Override
	protected double getDiscountValue(String sequenceStringPattern,
			long sequenceCount) {
		String stringPatternForBitcount = sequenceStringPattern.replaceAll("_",
				"0");
		if (Integer.bitCount(PatternTransformer
				.getIntPattern(PatternTransformer
						.getBooleanPattern(stringPatternForBitcount))) > 1) {
			// not lowest order
			if (sequenceCount == 1) {
				return this.discountTypeValuesMap.get(sequenceStringPattern)
						.get("D1");
			}
			if (sequenceCount == 2) {
				return this.discountTypeValuesMap.get(sequenceStringPattern)
						.get("D2");
			}
			if (sequenceCount >= 3) {
				return this.discountTypeValuesMap.get(sequenceStringPattern)
						.get("D3+");
			}
			// count < 1
			return 0;
		} else {
			// lowest order
			return 0;
		}
	}

	@Override
	protected double calculateWeightNumerator(String continuationPattern,
			String sequence, int sequenceLength, String sequenceStringPattern) {
		// [0]=1+
		// [1]=1
		// [2]=2
		// [3]=3+
		return this.getDiscountValue(continuationPattern, 1)
				* this.calculateContinuationLast(sequence, sequenceLength,
						sequenceStringPattern, 1)
				+ this.getDiscountValue(continuationPattern, 2)
				* this.calculateContinuationLast(sequence, sequenceLength,
						sequenceStringPattern, 2)
				+ this.getDiscountValue(continuationPattern, 3)
				* this.calculateContinuationLast(sequence, sequenceLength,
						sequenceStringPattern, 3);
	}
}
