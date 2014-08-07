package de.glmtk.smoothing.legacy3;

import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;

import de.glmtk.smoothing.Corpus;
import de.glmtk.smoothing.NGram;
import de.glmtk.smoothing.helper.TestCorpus;
import de.glmtk.smoothing.legacy3.CalculatingMode;
import de.glmtk.smoothing.legacy3.estimating.Estimator;
import de.glmtk.smoothing.legacy3.helper.AbstractEstimatorTest;

public class FixedHistoryPropabilitySumEqualsOneTest extends
        AbstractEstimatorTest {

    private static final Logger LOGGER = LogManager
            .getLogger(FixedHistoryPropabilitySumEqualsOneTest.class);

    @Override
    protected void testEstimator(
            String testName,
            Estimator estimator,
            int maxOrder) {
        LOGGER.info("===== {} =====", testName);
        testEstimatorCalculatingMode(estimator, CalculatingMode.SKIP, maxOrder);
        testEstimatorCalculatingMode(estimator, CalculatingMode.DELETE,
                maxOrder);
    }

    private void testEstimatorCalculatingMode(
            Estimator estimator,
            CalculatingMode calculatingMode,
            int maxOrder) {
        LOGGER.info("=== {}", calculatingMode);
        estimator.setCalculatingMode(calculatingMode);
        testEstimatorCalculatingModeCorpus(estimator, calculatingMode,
                abcCorpus, abcTestCorpus, maxOrder);
        testEstimatorCalculatingModeCorpus(estimator, calculatingMode,
                mobyDickCorpus, mobyDickTestCorpus, maxOrder);
    }

    private void testEstimatorCalculatingModeCorpus(
            Estimator estimator,
            CalculatingMode calculatingMode,
            Corpus corpus,
            TestCorpus testCorpus,
            int maxOrder) {
        LOGGER.info("# {} corpus", testCorpus.getWorkingDir().getFileName());

        estimator.setCorpus(corpus);
        for (int order = 1; order != maxOrder + 1; ++order) {
            LOGGER.info("n={}", order);
            for (int i = 0; i != (int) Math.pow(testCorpus.getWords().length,
                    order - 1); ++i) {
                List<String> history = testCorpus.getSequenceList(i, order - 1);

                double sum = 0;
                for (int j = 0; j != testCorpus.getWords().length; ++j) {
                    List<String> sequence =
                            Arrays.asList(testCorpus.getWords()[j]);
                    sum +=
                            estimator.probability(new NGram(sequence),
                                    new NGram(history));
                }
                Assert.assertEquals(1.0, sum, 0.01);
            }
        }
        LOGGER.info("passed");
    }

}
