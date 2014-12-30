package de.glmtk.querying;

import static de.glmtk.common.NGram.SKP_NGRAM;
import static de.glmtk.querying.estimator.Estimators.ABS_UNIGRAM;
import static de.glmtk.querying.estimator.Estimators.BACKOFF_CMLE;
import static de.glmtk.querying.estimator.Estimators.BACKOFF_CMLE_NOREC;
import static de.glmtk.querying.estimator.Estimators.CMLE;
import static de.glmtk.querying.estimator.Estimators.COMB_MLE_CMLE;
import static de.glmtk.querying.estimator.Estimators.CONT_UNIGRAM;
import static de.glmtk.querying.estimator.Estimators.DIFF_INTERPOL_ABS_DISCOUNT_MLE_DEL;
import static de.glmtk.querying.estimator.Estimators.DIFF_INTERPOL_ABS_DISCOUNT_MLE_DEL_FRONT;
import static de.glmtk.querying.estimator.Estimators.DIFF_INTERPOL_ABS_DISCOUNT_MLE_DEL_FRONT_NOREC;
import static de.glmtk.querying.estimator.Estimators.DIFF_INTERPOL_ABS_DISCOUNT_MLE_DEL_NOREC;
import static de.glmtk.querying.estimator.Estimators.DIFF_INTERPOL_ABS_DISCOUNT_MLE_SKP;
import static de.glmtk.querying.estimator.Estimators.DIFF_INTERPOL_ABS_DISCOUNT_MLE_SKP_AND_DEL;
import static de.glmtk.querying.estimator.Estimators.DIFF_INTERPOL_ABS_DISCOUNT_MLE_SKP_AND_DEL_NOREC;
import static de.glmtk.querying.estimator.Estimators.DIFF_INTERPOL_ABS_DISCOUNT_MLE_SKP_NOREC;
import static de.glmtk.querying.estimator.Estimators.FMLE;
import static de.glmtk.querying.estimator.Estimators.INTERPOL_ABS_DISCOUNT_MLE_DEL;
import static de.glmtk.querying.estimator.Estimators.INTERPOL_ABS_DISCOUNT_MLE_DEL_NOREC;
import static de.glmtk.querying.estimator.Estimators.INTERPOL_ABS_DISCOUNT_MLE_SKP;
import static de.glmtk.querying.estimator.Estimators.INTERPOL_ABS_DISCOUNT_MLE_SKP_NOREC;
import static de.glmtk.querying.estimator.Estimators.MLE;
import static de.glmtk.querying.estimator.Estimators.UNIFORM;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.glmtk.common.NGram;
import de.glmtk.common.ProbMode;
import de.glmtk.querying.calculator.Calculator;
import de.glmtk.querying.calculator.SequenceCalculator;
import de.glmtk.querying.estimator.Estimator;
import de.glmtk.testutil.EstimatorTestRunner;
import de.glmtk.testutil.EstimatorTestRunner.EstimatorTestParameters;
import de.glmtk.testutil.EstimatorTestRunner.EstimatorTestParams;
import de.glmtk.testutil.EstimatorTestRunner.IgnoreProbMode;
import de.glmtk.testutil.LoggingTest;
import de.glmtk.testutil.TestCorpus;

@RunWith(EstimatorTestRunner.class)
@IgnoreProbMode(ProbMode.COND)
public class EstimatorTest extends LoggingTest {

    private static final Logger LOGGER = LogManager
            .getFormatterLogger(EstimatorTest.class);

    private static final List<TestCorpus> TEST_CORPORA = Arrays.asList(
            TestCorpus.ABC, TestCorpus.MOBYDICK);

    private static final int HIGHEST_ORDER = 5;

    @EstimatorTestParameters
    public static Iterable<EstimatorTestParams> data() {
        return Arrays.asList(
                //@formatter:off
                new EstimatorTestParams(UNIFORM, false, 0, HIGHEST_ORDER),
                new EstimatorTestParams(ABS_UNIGRAM, false, 0, HIGHEST_ORDER),
                new EstimatorTestParams(CONT_UNIGRAM, false, 0, HIGHEST_ORDER - 1),

                // Fractions Estimators
                new EstimatorTestParams(MLE, false, HIGHEST_ORDER, HIGHEST_ORDER),
                new EstimatorTestParams(FMLE, false, 0, HIGHEST_ORDER),
                new EstimatorTestParams(CMLE, true, HIGHEST_ORDER - 1, HIGHEST_ORDER - 1),

                // Backoff Estimators
                new EstimatorTestParams(BACKOFF_CMLE_NOREC, true, HIGHEST_ORDER - 1, HIGHEST_ORDER - 1),
                new EstimatorTestParams(BACKOFF_CMLE, true, HIGHEST_ORDER - 1, HIGHEST_ORDER - 1),

                // Interpolation Estimators
                new EstimatorTestParams(INTERPOL_ABS_DISCOUNT_MLE_SKP_NOREC, false, HIGHEST_ORDER, HIGHEST_ORDER),
                new EstimatorTestParams(INTERPOL_ABS_DISCOUNT_MLE_DEL_NOREC, false, HIGHEST_ORDER, HIGHEST_ORDER),
                new EstimatorTestParams(INTERPOL_ABS_DISCOUNT_MLE_SKP, false, HIGHEST_ORDER, HIGHEST_ORDER),
                new EstimatorTestParams(INTERPOL_ABS_DISCOUNT_MLE_DEL, false, HIGHEST_ORDER, HIGHEST_ORDER),

                // DiffInterpolation Estimators
                new EstimatorTestParams(DIFF_INTERPOL_ABS_DISCOUNT_MLE_SKP_NOREC, false, HIGHEST_ORDER, HIGHEST_ORDER),
                new EstimatorTestParams(DIFF_INTERPOL_ABS_DISCOUNT_MLE_DEL_NOREC, false, HIGHEST_ORDER, HIGHEST_ORDER),
                new EstimatorTestParams(DIFF_INTERPOL_ABS_DISCOUNT_MLE_DEL_FRONT_NOREC, false, HIGHEST_ORDER, HIGHEST_ORDER),
                new EstimatorTestParams(DIFF_INTERPOL_ABS_DISCOUNT_MLE_SKP_AND_DEL_NOREC, false, HIGHEST_ORDER, HIGHEST_ORDER),
                new EstimatorTestParams(DIFF_INTERPOL_ABS_DISCOUNT_MLE_SKP, false, HIGHEST_ORDER, HIGHEST_ORDER),
                new EstimatorTestParams(DIFF_INTERPOL_ABS_DISCOUNT_MLE_DEL, false, HIGHEST_ORDER, HIGHEST_ORDER),
                new EstimatorTestParams(DIFF_INTERPOL_ABS_DISCOUNT_MLE_DEL_FRONT, false, HIGHEST_ORDER, HIGHEST_ORDER),
                // HIGHEST_ORDER should actually also work, but takes far to long to calculate.
                new EstimatorTestParams(DIFF_INTERPOL_ABS_DISCOUNT_MLE_SKP_AND_DEL, false, HIGHEST_ORDER - 1, HIGHEST_ORDER - 1),

                // Combination Estimators
                new EstimatorTestParams(COMB_MLE_CMLE, true, 0, HIGHEST_ORDER - 1)
                //@formatter:on
                );
    }

    private Estimator estimator;

    private boolean continuationEstimator;

    private ProbMode probMode;

    private int maxOrder;

    public EstimatorTest(
            Estimator estimator,
            boolean continuationEstimator,
            ProbMode probMode,
            int maxOrder) {
        LOGGER.info("====== %s (%s)", estimator.getName(), probMode);

        this.estimator = estimator;
        this.continuationEstimator = continuationEstimator;
        this.probMode = probMode;
        this.maxOrder = maxOrder;
    }

    @Test
    public void testNGramProbabilitiesSum() throws IOException {
        LOGGER.info("=== Test NGram Probabilities Sum");
        Calculator calculator = new SequenceCalculator();
        calculator.setEstimator(estimator);
        calculator.setProbMode(probMode);
        for (TestCorpus testCorpus : TEST_CORPORA) {
            LOGGER.info("# %s corpus", testCorpus.getCorpusName());

            estimator.setCountCache(testCorpus.getCountCache());

            for (int order = 1; order != maxOrder + 1; ++order) {
                double sum = 0;
                for (int i = 0; i != (int) Math.pow(
                        testCorpus.getWords().length, order); ++i) {
                    List<String> sequence =
                            testCorpus.getSequenceList(i, order);
                    sum += calculator.probability(sequence);
                }
                try {
                    Assert.assertEquals(1.0, sum, 0.01);
                } catch (AssertionError e) {
                    LOGGER.error("n=%s: sum = %s fail", order, sum);
                    throw e;
                }
                LOGGER.info("n=%s: sum = %s", order, sum);
            }
        }
    }

    @Test
    public void testFixedHistorySum() throws IOException {
        LOGGER.info("=== Test Fixed History Sum");

        for (TestCorpus testCorpus : TEST_CORPORA) {
            LOGGER.info("# %s corpus", testCorpus.getCorpusName());

            estimator.setCountCache(testCorpus.getCountCache());

            for (int order = 1; order != maxOrder + 1; ++order) {
                LOGGER.info("n=%s", order);
                for (int i = 0; i != (int) Math.pow(
                        testCorpus.getWords().length, order - 1); ++i) {
                    NGram history =
                            new NGram(testCorpus.getSequenceList(i, order - 1));

                    double sum = 0;
                    for (int j = 0; j != testCorpus.getWords().length; ++j) {
                        NGram sequence =
                                new NGram(
                                        Arrays.asList(testCorpus.getWords()[j]));
                        sum += estimator.probability(sequence, history);
                    }

                    try {
                        switch (probMode) {
                            case COND:
                                NGram checkHistory = history.concat(SKP_NGRAM);
                                if (continuationEstimator) {
                                    checkHistory =
                                            SKP_NGRAM.concat(checkHistory);
                                }
                                if (checkHistory.seen(testCorpus
                                        .getCountCache())) {
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
                    } catch (AssertionError e) {
                        LOGGER.error("history = %s, sum = %s fail", history,
                                sum);
                        throw e;
                    }
                    LOGGER.debug("history = %s, sum = %s", history, sum);
                }
            }
        }
    }

}
