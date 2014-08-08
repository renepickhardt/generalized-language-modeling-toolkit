package de.glmtk.smoothing;

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
            ProbMode[] probModes,
            int maxOrder,
            boolean continuationEstimator) {
        LOGGER.info("===== {} =====", estimatorName);
        NGramProbabilityCalculator calculator =
                new NGramProbabilityCalculator();
        calculator.setEstimator(estimator);
        for (ProbMode probMode : probModes) {
            LOGGER.info("=== {}", probMode);
            calculator.setProbMode(probMode);
            testEstimatorCalculatorCorpus(estimator, calculator, abcCorpus,
                    abcTestCorpus, maxOrder);
            testEstimatorCalculatorCorpus(estimator, calculator,
                    mobyDickCorpus, mobyDickTestCorpus, maxOrder);
        }
    }

    private void testEstimatorCalculatorCorpus(
            Estimator estimator,
            NGramProbabilityCalculator calculator,
            Corpus corpus,
            TestCorpus testCorpus,
            int maxOrder) {
        LOGGER.info("# {} corpus", testCorpus.getWorkingDir().getFileName());

        estimator.setCorpus(corpus);

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
