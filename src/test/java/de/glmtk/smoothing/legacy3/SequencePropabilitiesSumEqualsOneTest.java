package de.glmtk.smoothing.legacy3;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;

import de.glmtk.smoothing.Corpus;
import de.glmtk.smoothing.helper.TestCorpus;
import de.glmtk.smoothing.legacy3.CalculatingMode;
import de.glmtk.smoothing.legacy3.SequenceCalculator;
import de.glmtk.smoothing.legacy3.estimating.Estimator;
import de.glmtk.smoothing.legacy3.helper.AbstractEstimatorTest;

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
        SequenceCalculator sequenceCalculator = new SequenceCalculator();
        testEstimatorCalculator(estimator, sequenceCalculator,
                CalculatingMode.SKIP, maxOrder);
        testEstimatorCalculator(estimator, sequenceCalculator,
                CalculatingMode.DELETE, maxOrder);
    }

    private void testEstimatorCalculator(
            Estimator estimator,
            SequenceCalculator sequenceCalculator,
            CalculatingMode calculatingMode,
            int maxOrder) {
        LOGGER.info("=== {}", calculatingMode);
        sequenceCalculator.setCalculatingMode(calculatingMode);
        testEstimatorCalculatorCorpus(estimator, sequenceCalculator, abcCorpus,
                abcTestCorpus, maxOrder);
        testEstimatorCalculatorCorpus(estimator, sequenceCalculator,
                mobyDickCorpus, mobyDickTestCorpus, maxOrder);
    }

    private void testEstimatorCalculatorCorpus(
            Estimator estimator,
            SequenceCalculator sequenceCalculator,
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
                sum += sequenceCalculator.propability(estimator, sequence);
            }
            LOGGER.info("n={}: sum = {}", order, sum);
            Assert.assertEquals(1, sum, 0.01);
        }
        LOGGER.info("passed");
    }

}
