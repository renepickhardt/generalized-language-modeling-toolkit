package de.glmtk.smoothing;

import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;

import de.glmtk.smoothing.estimator.Estimator;
import de.glmtk.smoothing.helper.AbstractEstimatorTest;
import de.glmtk.smoothing.helper.TestCorpus;

public class NGramProbabilitiesSumTest extends AbstractEstimatorTest {

    private static final Logger LOGGER = LogManager
            .getLogger(NGramProbabilitiesSumTest.class);

    @Override
    protected void testEstimator(
            String estimatorName,
            Estimator estimator,
            ProbMode probMode,
            int maxOrder,
            boolean continuationEstimator) throws IOException {
        LOGGER.info("===== {} ({}) =====", estimatorName, probMode);
        NGramProbabilityCalculator calculator =
                new NGramProbabilityCalculator();
        calculator.setEstimator(estimator);
        calculator.setProbMode(probMode);
        testEstimatorCalculatorCorpus(estimator, calculator, TestCorpus.ABC,
                maxOrder);
        testEstimatorCalculatorCorpus(estimator, calculator,
                TestCorpus.MOBY_DICK, maxOrder);
    }

    private void testEstimatorCalculatorCorpus(
            Estimator estimator,
            NGramProbabilityCalculator calculator,
            TestCorpus testCorpus,
            int maxOrder) throws IOException {
        LOGGER.info("=== {} corpus", testCorpus.getCorpusName());

        estimator.setCountCache(testCorpus.getCountCache());

        for (int order = 1; order != maxOrder + 1; ++order) {
            double sum = 0;
            for (int i = 0; i != (int) Math.pow(testCorpus.getWords().length,
                    order); ++i) {
                List<String> sequence = testCorpus.getSequenceList(i, order);
                sum += calculator.probability(sequence);
            }
            LOGGER.info("n={}: sum = {}", order, sum);
            Assert.assertEquals(1.0, sum, 0.01);
        }

        LOGGER.info("passed");
    }

}
