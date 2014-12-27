package de.glmtk.querying;

import static de.glmtk.querying.estimator.Estimators.ABS_UNIGRAM;
import static de.glmtk.querying.estimator.Estimators.BACKOFF_CMLE;
import static de.glmtk.querying.estimator.Estimators.BACKOFF_CMLE_REC;
import static de.glmtk.querying.estimator.Estimators.CMLE;
import static de.glmtk.querying.estimator.Estimators.COMB_MLE_CMLE;
import static de.glmtk.querying.estimator.Estimators.CONT_UNIGRAM;
import static de.glmtk.querying.estimator.Estimators.DIFF_INTERPOL_ABS_DISCOUNT_MLE_DEL;
import static de.glmtk.querying.estimator.Estimators.DIFF_INTERPOL_ABS_DISCOUNT_MLE_DEL_FRONT;
import static de.glmtk.querying.estimator.Estimators.DIFF_INTERPOL_ABS_DISCOUNT_MLE_DEL_FRONT_REC;
import static de.glmtk.querying.estimator.Estimators.DIFF_INTERPOL_ABS_DISCOUNT_MLE_DEL_REC;
import static de.glmtk.querying.estimator.Estimators.DIFF_INTERPOL_ABS_DISCOUNT_MLE_SKP;
import static de.glmtk.querying.estimator.Estimators.DIFF_INTERPOL_ABS_DISCOUNT_MLE_SKP_AND_DEL;
import static de.glmtk.querying.estimator.Estimators.DIFF_INTERPOL_ABS_DISCOUNT_MLE_SKP_AND_DEL_REC;
import static de.glmtk.querying.estimator.Estimators.DIFF_INTERPOL_ABS_DISCOUNT_MLE_SKP_REC;
import static de.glmtk.querying.estimator.Estimators.FMLE;
import static de.glmtk.querying.estimator.Estimators.INTERPOL_ABS_DISCOUNT_MLE_DEL;
import static de.glmtk.querying.estimator.Estimators.INTERPOL_ABS_DISCOUNT_MLE_DEL_REC;
import static de.glmtk.querying.estimator.Estimators.INTERPOL_ABS_DISCOUNT_MLE_SKP;
import static de.glmtk.querying.estimator.Estimators.INTERPOL_ABS_DISCOUNT_MLE_SKP_REC;
import static de.glmtk.querying.estimator.Estimators.MLE;
import static de.glmtk.querying.estimator.Estimators.UNIFORM;
import static de.glmtk.utils.NGram.SKP_NGRAM;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.glmtk.querying.calculator.Calculator;
import de.glmtk.querying.calculator.SentenceCalculator;
import de.glmtk.querying.estimator.Estimator;
import de.glmtk.testutils.EstimatorTestRunner;
import de.glmtk.testutils.EstimatorTestRunner.EstimatorTestParameters;
import de.glmtk.testutils.EstimatorTestRunner.EstimatorTestParams;
import de.glmtk.testutils.TestCorporaTest;
import de.glmtk.testutils.TestCorpus;
import de.glmtk.utils.NGram;

@RunWith(EstimatorTestRunner.class)
public class EstimatorTest extends TestCorporaTest {

    private static final Logger LOGGER = LogManager
            .getLogger(EstimatorTest.class);

    private static final List<TestCorpus> TEST_CORPORA = Arrays.asList(
            TestCorpus.ABC, TestCorpus.MOBY_DICK);

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
                new EstimatorTestParams(BACKOFF_CMLE, true, HIGHEST_ORDER - 1, HIGHEST_ORDER - 1),
                new EstimatorTestParams(BACKOFF_CMLE_REC, true, HIGHEST_ORDER - 1, HIGHEST_ORDER - 1),

                // Interpolation Estimators
                new EstimatorTestParams(INTERPOL_ABS_DISCOUNT_MLE_SKP, false, HIGHEST_ORDER, HIGHEST_ORDER),
                new EstimatorTestParams(INTERPOL_ABS_DISCOUNT_MLE_DEL, false, HIGHEST_ORDER, HIGHEST_ORDER),
                new EstimatorTestParams(INTERPOL_ABS_DISCOUNT_MLE_SKP_REC, false, HIGHEST_ORDER, HIGHEST_ORDER),
                new EstimatorTestParams(INTERPOL_ABS_DISCOUNT_MLE_DEL_REC, false, HIGHEST_ORDER, HIGHEST_ORDER),

                // DiffInterpolation Estimators
                new EstimatorTestParams(DIFF_INTERPOL_ABS_DISCOUNT_MLE_SKP, false, HIGHEST_ORDER, HIGHEST_ORDER),
                new EstimatorTestParams(DIFF_INTERPOL_ABS_DISCOUNT_MLE_DEL, false, HIGHEST_ORDER, HIGHEST_ORDER),
                new EstimatorTestParams(DIFF_INTERPOL_ABS_DISCOUNT_MLE_DEL_FRONT, false, HIGHEST_ORDER, HIGHEST_ORDER),
                new EstimatorTestParams(DIFF_INTERPOL_ABS_DISCOUNT_MLE_SKP_AND_DEL, false, HIGHEST_ORDER, HIGHEST_ORDER),
                new EstimatorTestParams(DIFF_INTERPOL_ABS_DISCOUNT_MLE_SKP_REC, false, HIGHEST_ORDER, HIGHEST_ORDER),
                new EstimatorTestParams(DIFF_INTERPOL_ABS_DISCOUNT_MLE_DEL_REC, false, HIGHEST_ORDER, HIGHEST_ORDER),
                new EstimatorTestParams(DIFF_INTERPOL_ABS_DISCOUNT_MLE_DEL_FRONT_REC, false, HIGHEST_ORDER, HIGHEST_ORDER),

                // HIGHEST_ORDER should actually also work,
                // but takes far to long to calculate.
                new EstimatorTestParams(DIFF_INTERPOL_ABS_DISCOUNT_MLE_SKP_AND_DEL_REC, false, HIGHEST_ORDER - 1, HIGHEST_ORDER - 1),

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
            String estimatorName,
            Estimator estimator,
            boolean continuationEstimator,
            ProbMode probMode,
            int maxOrder) {
        LOGGER.info("====== {} ({})", estimatorName, probMode);

        this.estimator = estimator;
        this.continuationEstimator = continuationEstimator;
        this.probMode = probMode;
        this.maxOrder = maxOrder;
    }

    @Test
    public void testNGramProbabilitiesSum() throws IOException {
        LOGGER.info("=== Test NGram Probabilities Sum");
        Calculator calculator = new SentenceCalculator();
        calculator.setEstimator(estimator);
        calculator.setProbMode(probMode);
        for (TestCorpus testCorpus : TEST_CORPORA) {
            LOGGER.info("# {} corpus", testCorpus.getCorpusName());

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
                    LOGGER.error("n={}: sum = {} fail", order, sum);
                    throw e;
                }
                LOGGER.info("n={}: sum = {}", order, sum);
            }
        }
    }

    @Test
    public void testFixedHistorySum() throws IOException {
        LOGGER.info("=== Test Fixed History Sum");

        for (TestCorpus testCorpus : TEST_CORPORA) {
            LOGGER.info("# {} corpus", testCorpus.getCorpusName());

            estimator.setCountCache(testCorpus.getCountCache());

            for (int order = 1; order != maxOrder + 1; ++order) {
                LOGGER.info("n={}", order);
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
                        LOGGER.error("history = {}, sum = {} fail", history,
                                sum);
                        throw e;
                    }
                    LOGGER.debug("history = {}, sum = {}", history, sum);
                }
            }
        }
    }

}
