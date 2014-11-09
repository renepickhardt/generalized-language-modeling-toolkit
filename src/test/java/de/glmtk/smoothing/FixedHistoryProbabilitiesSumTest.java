package de.glmtk.smoothing;

import java.io.IOException;
import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;

import de.glmtk.smoothing.estimator.Estimator;
import de.glmtk.smoothing.helper.AbstractEstimatorTest;
import de.glmtk.smoothing.helper.TestCorpus;

public class FixedHistoryProbabilitiesSumTest extends AbstractEstimatorTest {

    private static final Logger LOGGER = LogManager
            .getLogger(FixedHistoryProbabilitiesSumTest.class);

    @Override
    protected void testEstimator(
            String estimatorName,
            Estimator estimator,
            ProbMode probMode,
            int maxOrder,
            boolean continuationEstimator) throws IOException {
        LOGGER.info("===== {} ({}) =====", estimatorName, probMode);
        estimator.setProbMode(probMode);
        testEstimatorCorpus(estimator, probMode, TestCorpus.ABC, maxOrder,
                continuationEstimator);
        testEstimatorCorpus(estimator, probMode, TestCorpus.MOBY_DICK,
                maxOrder, continuationEstimator);
    }

    private void testEstimatorCorpus(
            Estimator estimator,
            ProbMode probMode,
            TestCorpus testCorpus,
            int maxOrder,
            boolean conntinuationEstimator) throws IOException {
        LOGGER.info("=== {} corpus", testCorpus.getCorpusName());

        estimator.setCountCache(testCorpus.getCountCache());

        for (int order = 1; order != maxOrder + 1; ++order) {
            LOGGER.info("n={}", order);
            for (int i = 0; i != (int) Math.pow(testCorpus.getWords().length,
                    order - 1); ++i) {
                NGram history =
                        new NGram(testCorpus.getSequenceList(i, order - 1));

                double sum = 0;
                for (int j = 0; j != testCorpus.getWords().length; ++j) {
                    NGram sequence =
                            new NGram(Arrays.asList(testCorpus.getWords()[j]));
                    sum += estimator.probability(sequence, history);
                }

                LOGGER.debug("history = {}, sum = {}", history, sum);

                switch (probMode) {
                    case COND:
                        NGram checkHistory =
                        history.concat(NGram.SKIPPED_WORD_NGRAM);
                        if (conntinuationEstimator) {
                            checkHistory =
                                    NGram.SKIPPED_WORD_NGRAM
                                    .concat(checkHistory);
                        }
                        if (checkHistory.seen(testCorpus.getCountCache())) {
                            if (Math.abs(0.0 - sum) <= 0.01) {
                                System.out.println(sum);
                                System.out.println(history);
                            }
                            Assert.assertEquals(1.0, sum, 0.01);
                        } else {
                            Assert.assertEquals(0.0, sum, 0.01);
                        }
                        break;
                    case MARG:
                        Assert.assertEquals(1.0, sum, 0.01);
                        break;
                    default:
                        throw new IllegalStateException();
                }
            }
        }

        LOGGER.info("passed");
    }
}
