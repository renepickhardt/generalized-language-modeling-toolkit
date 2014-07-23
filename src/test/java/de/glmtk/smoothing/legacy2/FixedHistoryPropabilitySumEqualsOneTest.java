package de.glmtk.smoothing.legacy2;

import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;

import de.glmtk.smoothing.Corpus;
import de.glmtk.smoothing.helper.TestCorpus;
import de.glmtk.smoothing.legacy2.Estimator;

public class FixedHistoryPropabilitySumEqualsOneTest extends
        AbstractEstimatorTest {

    protected static Logger logger = LogManager
            .getLogger(FixedHistoryPropabilitySumEqualsOneTest.class);

    @Override
    protected void testEstimator(
            String testName,
            Estimator estimator,
            int maxOrder) {
        logger.info("===== {} =====", testName);
        testEstimatorCorpus(estimator, abcCorpus, abcTestCorpus, maxOrder);
        testEstimatorCorpus(estimator, mobyDickCorpus, mobyDickTestCorpus,
                maxOrder);
        // add more corpora here
    }

    protected void testEstimatorCorpus(
            Estimator estimator,
            Corpus corpus,
            TestCorpus testCorpus,
            int maxOrder) {
        logger.info("# {} corpus", testCorpus.getWorkingDir().getFileName());

        estimator.setCorpus(corpus);
        for (int order = 1; order != maxOrder; ++order) {
            logger.info("n={}", order);
            for (int i = 0; i != (int) Math.pow(testCorpus.getWords().length,
                    order - 1); ++i) {
                List<String> history = testCorpus.getSequenceList(i, order - 1);

                double sum = 0;
                for (int j = 0; j != testCorpus.getWords().length; ++j) {
                    List<String> sequence =
                            Arrays.asList(testCorpus.getWords()[j]);
                    sum += estimator.propabilityCond(sequence, history);
                }
                Assert.assertEquals(1.0, sum, 0.01);
            }
        }
        logger.info("passed");
    }
}
