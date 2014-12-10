package de.glmtk.querying;

import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;

import de.glmtk.querying.calculator.SentenceCalculator;
import de.glmtk.querying.estimator.Estimator;
import de.glmtk.querying.estimator.Estimators;
import de.glmtk.querying.helper.AbstractEstimatorTest;
import de.glmtk.querying.helper.TestCorpus;

public class NGramProbabilitiesSumTest extends AbstractEstimatorTest {

    private static final Logger LOGGER = LogManager
            .getLogger(NGramProbabilitiesSumTest.class);

    @Override
    protected void testEstimator(
            Estimator estimator,
            ProbMode probMode,
            int maxOrder,
            boolean continuationEstimator) throws IOException {
        LOGGER.info("===== {} ({}) =====", Estimators.getName(estimator),
                probMode);
        SentenceCalculator calculator =
                new SentenceCalculator();
        calculator.setEstimator(estimator);
        calculator.setProbMode(probMode);
        testEstimatorCalculatorCorpus(estimator, calculator, TestCorpus.ABC,
                maxOrder);
        testEstimatorCalculatorCorpus(estimator, calculator,
                TestCorpus.MOBY_DICK, maxOrder);
    }

    private void testEstimatorCalculatorCorpus(
            Estimator estimator,
            SentenceCalculator calculator,
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
            try {
                Assert.assertEquals(1.0, sum, 0.01);
            } catch (AssertionError e) {
                LOGGER.error("n={}: sum = {} fail", order, sum);
                throw e;
            }
            LOGGER.info("n={}: sum = {}", order, sum);
        }
    }

}
