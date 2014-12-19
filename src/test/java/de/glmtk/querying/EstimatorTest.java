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
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import de.glmtk.querying.ProbMode;
import de.glmtk.querying.calculator.Calculator;
import de.glmtk.querying.calculator.SentenceCalculator;
import de.glmtk.querying.estimator.Estimator;
import de.glmtk.querying.estimator.Estimators;
import de.glmtk.testutils.TestCorporaTest;
import de.glmtk.testutils.TestCorpus;
import de.glmtk.utils.NGram;

@RunWith(Parameterized.class)
public class EstimatorTest extends TestCorporaTest {

    private static final Logger LOGGER = LogManager
            .getLogger(EstimatorTest.class);

    private static final int HIGHEST_TEST_ORDER = 5;

    private static final List<Object[]> testData;
    static {
        testData = new LinkedList<Object[]>();

        // Substitute Estimators
        addTestDataSet(UNIFORM, ProbMode.MARG, HIGHEST_TEST_ORDER, false);
        addTestDataSet(ABS_UNIGRAM, ProbMode.MARG, HIGHEST_TEST_ORDER, false);
        addTestDataSet(CONT_UNIGRAM, ProbMode.MARG, HIGHEST_TEST_ORDER - 1,
                false);

        // Fractions Estimators
        addTestDataSet(MLE, ProbMode.COND, HIGHEST_TEST_ORDER, false);
        addTestDataSet(MLE, ProbMode.MARG, HIGHEST_TEST_ORDER, false);
        addTestDataSet(FMLE, ProbMode.MARG, HIGHEST_TEST_ORDER, false);
        addTestDataSet(CMLE, ProbMode.COND, HIGHEST_TEST_ORDER - 1, true);
        addTestDataSet(CMLE, ProbMode.MARG, HIGHEST_TEST_ORDER - 1, true);

        // Backoff Estimators
        addTestDataSet(BACKOFF_CMLE, ProbMode.COND, HIGHEST_TEST_ORDER - 1,
                true);
        addTestDataSet(BACKOFF_CMLE, ProbMode.MARG, HIGHEST_TEST_ORDER - 1,
                true);
        addTestDataSet(BACKOFF_CMLE_REC, ProbMode.MARG, HIGHEST_TEST_ORDER - 1,
                true);
        addTestDataSet(BACKOFF_CMLE_REC, ProbMode.COND, HIGHEST_TEST_ORDER - 1,
                true);

        // Interpolation Estimators
        addTestDataSet(INTERPOL_ABS_DISCOUNT_MLE_SKP, ProbMode.COND,
                HIGHEST_TEST_ORDER, false);
        addTestDataSet(INTERPOL_ABS_DISCOUNT_MLE_SKP, ProbMode.MARG,
                HIGHEST_TEST_ORDER, false);
        addTestDataSet(INTERPOL_ABS_DISCOUNT_MLE_DEL, ProbMode.COND,
                HIGHEST_TEST_ORDER, false);
        addTestDataSet(INTERPOL_ABS_DISCOUNT_MLE_DEL, ProbMode.MARG,
                HIGHEST_TEST_ORDER, false);
        addTestDataSet(INTERPOL_ABS_DISCOUNT_MLE_SKP_REC, ProbMode.COND,
                HIGHEST_TEST_ORDER, false);
        addTestDataSet(INTERPOL_ABS_DISCOUNT_MLE_SKP_REC, ProbMode.MARG,
                HIGHEST_TEST_ORDER, false);
        addTestDataSet(INTERPOL_ABS_DISCOUNT_MLE_DEL_REC, ProbMode.COND,
                HIGHEST_TEST_ORDER, false);
        addTestDataSet(INTERPOL_ABS_DISCOUNT_MLE_DEL_REC, ProbMode.MARG,
                HIGHEST_TEST_ORDER, false);

        // DiffInterpolation Estimators
        addTestDataSet(DIFF_INTERPOL_ABS_DISCOUNT_MLE_SKP, ProbMode.COND,
                HIGHEST_TEST_ORDER, false);
        addTestDataSet(DIFF_INTERPOL_ABS_DISCOUNT_MLE_SKP, ProbMode.MARG,
                HIGHEST_TEST_ORDER, false);
        addTestDataSet(DIFF_INTERPOL_ABS_DISCOUNT_MLE_DEL, ProbMode.COND,
                HIGHEST_TEST_ORDER, false);
        addTestDataSet(DIFF_INTERPOL_ABS_DISCOUNT_MLE_DEL, ProbMode.MARG,
                HIGHEST_TEST_ORDER, false);
        addTestDataSet(DIFF_INTERPOL_ABS_DISCOUNT_MLE_DEL_FRONT, ProbMode.COND,
                HIGHEST_TEST_ORDER, false);
        addTestDataSet(DIFF_INTERPOL_ABS_DISCOUNT_MLE_DEL_FRONT, ProbMode.MARG,
                HIGHEST_TEST_ORDER, false);
        addTestDataSet(DIFF_INTERPOL_ABS_DISCOUNT_MLE_SKP_AND_DEL,
                ProbMode.COND, HIGHEST_TEST_ORDER, false);
        addTestDataSet(DIFF_INTERPOL_ABS_DISCOUNT_MLE_SKP_AND_DEL,
                ProbMode.MARG, HIGHEST_TEST_ORDER, false);
        addTestDataSet(DIFF_INTERPOL_ABS_DISCOUNT_MLE_SKP_REC, ProbMode.COND,
                HIGHEST_TEST_ORDER, false);
        addTestDataSet(DIFF_INTERPOL_ABS_DISCOUNT_MLE_SKP_REC, ProbMode.MARG,
                HIGHEST_TEST_ORDER, false);
        addTestDataSet(DIFF_INTERPOL_ABS_DISCOUNT_MLE_DEL_REC, ProbMode.COND,
                HIGHEST_TEST_ORDER, false);
        addTestDataSet(DIFF_INTERPOL_ABS_DISCOUNT_MLE_DEL_REC, ProbMode.MARG,
                HIGHEST_TEST_ORDER, false);
        addTestDataSet(DIFF_INTERPOL_ABS_DISCOUNT_MLE_DEL_FRONT_REC,
                ProbMode.COND, HIGHEST_TEST_ORDER, false);
        addTestDataSet(DIFF_INTERPOL_ABS_DISCOUNT_MLE_DEL_FRONT_REC,
                ProbMode.MARG, HIGHEST_TEST_ORDER, false);

        // HIGHEST_TEST_ORDER should actually also work,
        // but takes far to long to calculate.
        addTestDataSet(DIFF_INTERPOL_ABS_DISCOUNT_MLE_SKP_AND_DEL_REC,
                ProbMode.COND, HIGHEST_TEST_ORDER - 1, false);
        addTestDataSet(DIFF_INTERPOL_ABS_DISCOUNT_MLE_SKP_AND_DEL_REC,
                ProbMode.MARG, HIGHEST_TEST_ORDER - 1, false);

        // Combination Estimators
        addTestDataSet(COMB_MLE_CMLE, ProbMode.MARG, HIGHEST_TEST_ORDER - 1,
                true);
    }

    private static void addTestDataSet(
            Estimator estimator,
            ProbMode probMode,
            int maxOrder,
            boolean continuationEstimator) {
        testData.add(new Object[] {
                Estimators.getName(estimator), estimator, probMode, maxOrder,
                continuationEstimator
        });
    }

    @Parameters(
            name = "{index} {0} ({2})")
    public static Iterable<Object[]> data() {
        return testData;
    }

    private static final List<TestCorpus> TEST_CORPORA = Arrays.asList(
            TestCorpus.ABC, TestCorpus.MOBY_DICK);

    private Estimator estimator;

    private ProbMode probMode;

    private int maxOrder;

    private boolean continuationEstimator;

    public EstimatorTest(
            String estimatorName,
            Estimator estimator,
            ProbMode probMode,
            int maxOrder,
            boolean continuationEstimator) {
        LOGGER.info("====== {} ({})", estimatorName, probMode);

        this.estimator = estimator;
        this.probMode = probMode;
        this.maxOrder = maxOrder;
        this.continuationEstimator = continuationEstimator;
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
