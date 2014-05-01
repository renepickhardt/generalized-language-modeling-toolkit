package de.typology.smoothing;

import java.io.File;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.typology.patterns.PatternTransformer;

public class ModifiedKneserNeySmoother extends KneserNeySmoother {

    private static Logger logger = LogManager.getLogger();

    private double d1;

    private double d2;

    private double d3plus;

    public ModifiedKneserNeySmoother(
            File extractedSequenceDirectory,
            File absoluteDirectory,
            File continuationDirectory,
            String delimiter,
            int decimalPlaces) {
        super(extractedSequenceDirectory, absoluteDirectory,
                continuationDirectory, delimiter);

        discountTypesValuesMapFile =
                new File(this.absoluteDirectory.getParentFile()
                        .getAbsolutePath()
                        + "/discount-values-mod-kneser-ney.ser");

    }

    @Override
    protected HashMap<String, HashMap<String, Double>> calculateDiscountValues(
            HashMap<String, HashMap<String, Double>> discountTypeValuesMap,
            File workingDirectory) {
        for (File absoluteTypeDirectory : workingDirectory.listFiles()) {
            if (absoluteTypeDirectory.getName().contains("split")) {
                continue;
            }
            HashMap<String, Double> discountValuesMap =
                    new HashMap<String, Double>();
            long n1 =
                    Counter.countCountsInDirectory(1, absoluteTypeDirectory,
                            "<fs>");
            long n2 =
                    Counter.countCountsInDirectory(2, absoluteTypeDirectory,
                            "<fs>");
            long n3 =
                    Counter.countCountsInDirectory(3, absoluteTypeDirectory,
                            "<fs>");
            long n4 =
                    Counter.countCountsInDirectory(4, absoluteTypeDirectory,
                            "<fs>");
            logger.info("n1 for " + absoluteTypeDirectory.getName() + ":" + n1);
            logger.info("n2 for " + absoluteTypeDirectory.getName() + ":" + n2);
            logger.info("n3 for " + absoluteTypeDirectory.getName() + ":" + n3);
            logger.info("n4 for " + absoluteTypeDirectory.getName() + ":" + n4);
            double y = n1 / ((double) n1 + 2 * n2);
            d1 = 1 - 2 * y * ((double) n2 / (double) n1);
            d2 = 2 - 3 * y * ((double) n3 / (double) n2);
            d3plus = 3 - 4 * y * ((double) n4 / (double) n3);
            // this.d1plus = 0.5;
            logger.info("D1 for " + absoluteTypeDirectory.getName() + ":" + d1);
            logger.info("D2 for " + absoluteTypeDirectory.getName() + ":" + d2);
            logger.info("D3+ for " + absoluteTypeDirectory.getName() + ":"
                    + d3plus);
            discountValuesMap.put("D1", d1);
            discountValuesMap.put("D2", d2);
            discountValuesMap.put("D3+", d3plus);

            discountTypeValuesMap.put(absoluteTypeDirectory.getName(),
                    discountValuesMap);
        }
        return discountTypeValuesMap;

    }

    @Override
    protected double getDiscountValue(
            String sequenceStringPattern,
            long sequenceCount) {
        String stringPatternForBitcount =
                sequenceStringPattern.replaceAll("_", "0");
        if (Integer.bitCount(PatternTransformer
                .getIntPattern(PatternTransformer
                        .getBooleanPattern(stringPatternForBitcount))) > 1) {
            // not lowest order
            if (sequenceCount == 1) {
                return discountTypeValuesMap.get(sequenceStringPattern).get(
                        "D1");
            }
            if (sequenceCount == 2) {
                return discountTypeValuesMap.get(sequenceStringPattern).get(
                        "D2");
            }
            if (sequenceCount >= 3) {
                return discountTypeValuesMap.get(sequenceStringPattern).get(
                        "D3+");
            }
            // count < 1
            return 0;
        } else {
            // lowest order
            return 0;
        }
    }

    @Override
    protected double calculateWeightNumerator(
            String continuationPattern,
            String sequence,
            int sequenceLength,
            String sequenceStringPattern) {
        // [0]=1+
        // [1]=1
        // [2]=2
        // [3]=3+
        return getDiscountValue(continuationPattern, 1)
                * calculateContinuationLast(sequence, sequenceLength,
                        sequenceStringPattern, 1)
                + getDiscountValue(continuationPattern, 2)
                * calculateContinuationLast(sequence, sequenceLength,
                        sequenceStringPattern, 2)
                + getDiscountValue(continuationPattern, 3)
                * calculateContinuationLast(sequence, sequenceLength,
                        sequenceStringPattern, 3);
    }
}
