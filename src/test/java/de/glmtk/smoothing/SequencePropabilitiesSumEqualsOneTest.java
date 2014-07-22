package de.glmtk.smoothing;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;

import de.glmtk.smoothing.calculating.DeleteCalculator;
import de.glmtk.smoothing.calculating.SequenceCalculator;
import de.glmtk.smoothing.calculating.SkipCalculator;
import de.glmtk.smoothing.estimating.Estimator;

public class SequencePropabilitiesSumEqualsOneTest extends
        AbstractEstimatorTest {

    private static final Logger LOGGER = LogManager
            .getLogger(SequencePropabilitiesSumEqualsOneTest.class);

    @Override
    protected void testEstimator(
            String testName,
            Estimator estimator,
            int maxOrder) {
        LOGGER.info("===== {} =====", testName);
        testEstimatorCalculator(estimator, new SkipCalculator(), maxOrder);
        testEstimatorCalculator(estimator, new DeleteCalculator(), maxOrder);
    }

    private void testEstimatorCalculator(
            Estimator estimator,
            SequenceCalculator calculator,
            int maxOrder) {
        LOGGER.info("=== {}", calculator.getClass().getSimpleName());
        testEstimatorCalculatorCorpus(estimator, calculator, abcCorpus,
                abcTestCorpus, maxOrder);
        testEstimatorCalculatorCorpus(estimator, calculator, mobyDickCorpus,
                mobyDickTestCorpus, maxOrder);
    }

    private void testEstimatorCalculatorCorpus(
            Estimator estimator,
            SequenceCalculator calculator,
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
                sum += calculator.propability(estimator, sequence);
            }
            LOGGER.info("n={}: sum = {}", order, sum);
            Assert.assertEquals(1, sum, 0.01);
        }
        LOGGER.info("passed");
    }

}
