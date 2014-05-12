package de.typology.smoothing;

import java.io.File;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.typology.patterns.Pattern;

public class LegacyModifiedKneserNeySmoother extends LegacyKneserNeySmoother {

    private static Logger logger = LogManager.getLogger();

    private double d1;

    private double d2;

    private double d3plus;

    public LegacyModifiedKneserNeySmoother(
            File extractedSequenceDir,
            File absoluteDir,
            File continuationDir,
            String delimiter,
            int decimalPlaces) {
        super(extractedSequenceDir, absoluteDir,
                continuationDir, delimiter);

        discountTypesValuesMapFile =
                new File(this.absoluteDir.getParentFile()
                        .getAbsolutePath()
                        + "/discount-values-mod-kneser-ney.ser");

    }

    @Override
    protected HashMap<String, HashMap<String, Double>> calculateDiscountValues(
            HashMap<String, HashMap<String, Double>> discountTypeValuesMap,
            File workingDir) {
        for (File absoluteTypeDir : workingDir.listFiles()) {
            if (absoluteTypeDir.getName().contains("split")) {
                continue;
            }
            HashMap<String, Double> discountValuesMap =
                    new HashMap<String, Double>();
            long n1 =
                    LegacyCounter.countCountsInDir(1, absoluteTypeDir,
                            "<fs>");
            long n2 =
                    LegacyCounter.countCountsInDir(2, absoluteTypeDir,
                            "<fs>");
            long n3 =
                    LegacyCounter.countCountsInDir(3, absoluteTypeDir,
                            "<fs>");
            long n4 =
                    LegacyCounter.countCountsInDir(4, absoluteTypeDir,
                            "<fs>");
            logger.info("n1 for " + absoluteTypeDir.getName() + ":" + n1);
            logger.info("n2 for " + absoluteTypeDir.getName() + ":" + n2);
            logger.info("n3 for " + absoluteTypeDir.getName() + ":" + n3);
            logger.info("n4 for " + absoluteTypeDir.getName() + ":" + n4);
            double y = n1 / ((double) n1 + 2 * n2);
            d1 = 1 - 2 * y * ((double) n2 / (double) n1);
            d2 = 2 - 3 * y * ((double) n3 / (double) n2);
            d3plus = 3 - 4 * y * ((double) n4 / (double) n3);
            // this.d1plus = 0.5;
            logger.info("D1 for " + absoluteTypeDir.getName() + ":" + d1);
            logger.info("D2 for " + absoluteTypeDir.getName() + ":" + d2);
            logger.info("D3+ for " + absoluteTypeDir.getName() + ":"
                    + d3plus);
            discountValuesMap.put("D1", d1);
            discountValuesMap.put("D2", d2);
            discountValuesMap.put("D3+", d3plus);

            discountTypeValuesMap.put(absoluteTypeDir.getName(),
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
        if (new Pattern(stringPatternForBitcount).numCnt() > 1) {
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
