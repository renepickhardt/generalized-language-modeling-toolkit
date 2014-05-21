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

    private static TestCorpus testCorpus;

    private static Corpus corpus;

    @BeforeClass
    public static void setUpBeforeClass() throws IOException,
            InterruptedException {
        testCorpus = new MobyDickTestCorpus();
        corpus =
                new Corpus(testCorpus.getAbsoluteDir(),
                        testCorpus.getContinuationDir(), "\t");
    }

    @Test
    public void print() {
        MaximumLikelihoodEstimator mle = new MaximumLikelihoodEstimator(corpus);
        FalseMaximumLikelihoodEstimator fmle =
                new FalseMaximumLikelihoodEstimator(corpus);

        SkipCalculator skipMle = new SkipCalculator(mle);
        DeleteCalculator deleteMle = new DeleteCalculator(mle);
        DeleteCalculator deleteFmle = new DeleteCalculator(fmle);

        printPropabilities(skipMle, 5);
    }

    private void
        printPropabilities(PropabilityCalculator calculator, int length) {
        Map<Integer, Map<String, Double>> propabilitiesByLength =
                new LinkedHashMap<Integer, Map<String, Double>>();
        for (int i = 1; i != length + 1; ++i) {
            propabilitiesByLength.put(i,
                    calcSequencePropabilities(calculator, i));
        }

        logger.info(calculator.getClass().getSimpleName());
        for (Map<String, Double> propabilities : propabilitiesByLength.values()) {
            logger.info("---");
            printPropabilities(propabilities);
        }
        logger.info("===\n");
    }

    private Map<String, Double> calcSequencePropabilities(
            PropabilityCalculator calculator,
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
