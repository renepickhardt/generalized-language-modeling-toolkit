package de.typology.smoothing;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrintPropabilities {

    private static Logger logger = LoggerFactory
            .getLogger(PrintPropabilities.class);

    private static int MAX_LENGTH = 5;

    private static TestCorpus abcTestCorpus;

    private static Corpus abcCorpus;

    private static TestCorpus mobyDickTestCorpus;

    private static Corpus mobyDickCorpus;

    @BeforeClass
    public static void setUpBeforeClass() throws IOException,
            InterruptedException {
        abcTestCorpus = new AbcTestCorpus();
        abcCorpus = abcTestCorpus.getCorpus();
        mobyDickTestCorpus = new MobyDickTestCorpus();
        mobyDickCorpus = mobyDickTestCorpus.getCorpus();
    }

    @Test
    public void print() {
        MaximumLikelihoodEstimator mleAbc =
                new MaximumLikelihoodEstimator(abcCorpus);
        FalseMaximumLikelihoodEstimator fmleAbc =
                new FalseMaximumLikelihoodEstimator(abcCorpus);
        MaximumLikelihoodEstimator mleMobyDick =
                new MaximumLikelihoodEstimator(mobyDickCorpus);
        FalseMaximumLikelihoodEstimator fmleMobyDick =
                new FalseMaximumLikelihoodEstimator(mobyDickCorpus);

        logger.info("=== SkipMle ============================================");
        SkipCalculator skipMle;
        logger.info("# Abc Corpus");
        skipMle = new SkipCalculator(mleAbc);
        printPropabilities(skipMle, abcTestCorpus, 5);
        logger.info("# MobyDick Corpus");
        skipMle = new SkipCalculator(mleMobyDick);
        printPropabilities(skipMle, mobyDickTestCorpus, 5);

        logger.info("=== DeleteMle ==========================================");
        DeleteCalculator deleteMle;
        logger.info("# Abc Corpus");
        deleteMle = new DeleteCalculator(mleAbc);
        printPropabilities(deleteMle, abcTestCorpus, 5);
        logger.info("# MobyDick Corpus");
        deleteMle = new DeleteCalculator(mleMobyDick);
        printPropabilities(deleteMle, mobyDickTestCorpus, 5);

        logger.info("=== DeleteFmle =========================================");
        DeleteCalculator deleteFmle;
        logger.info("# Abc Corpus");
        deleteFmle = new DeleteCalculator(fmleAbc);
        printPropabilities(deleteFmle, abcTestCorpus, 5);
        logger.info("# MobyDick Corpus");
        deleteFmle = new DeleteCalculator(fmleMobyDick);
        printPropabilities(deleteFmle, mobyDickTestCorpus, 5);
    }

    private void printPropabilities(
            PropabilityCalculator calculator,
            TestCorpus testCorpus,
            int length) {
        Map<Integer, Map<String, Double>> propabilitiesByLength =
                new LinkedHashMap<Integer, Map<String, Double>>();
        for (int i = 1; i != length + 1; ++i) {
            propabilitiesByLength.put(i,
                    calcSequencePropabilities(calculator, testCorpus, i));
        }

        for (Map<String, Double> propabilities : propabilitiesByLength.values()) {
            printPropabilities(propabilities);
            logger.info("---");
        }
    }

    private Map<String, Double> calcSequencePropabilities(
            PropabilityCalculator calculator,
            TestCorpus testCorpus,
            int length) {
        Map<String, Double> propabilities = new LinkedHashMap<String, Double>();

        for (int i = 0; i != ((int) Math.pow(testCorpus.getWords().length,
                length)); ++i) {
            String sequence = testCorpus.getSequence(i, length);
            propabilities.put(sequence, calculator.propability(sequence));
        }

        return propabilities;
    }

    private void printPropabilities(Map<String, Double> propabilities) {
        double sum = 0;
        int cntZero = 0;
        int cntNonZero = 0;
        for (Map.Entry<String, Double> sequencePropability : propabilities
                .entrySet()) {
            String sequence = sequencePropability.getKey();
            double propability = sequencePropability.getValue();

            sum += propability;

            if (propability == 0.0) {
                ++cntZero;
            } else {
                ++cntNonZero;
                logger.info(sequence + " -> " + propability);
            }
        }
        logger.info("sum = " + sum + " ; cntZero = " + cntZero
                + " ; cntNonZero = " + cntNonZero);
    }
}
