/*
 * Generalized Language Modeling Toolkit (GLMTK)
 *
 * Copyright (C) 2014-2015 Lukas Schmelzeisen, Rene Pickhardt
 *
 * GLMTK is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * GLMTK is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * GLMTK. If not, see <http://www.gnu.org/licenses/>.
 *
 * See the AUTHORS file for contributors.
 */

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
import static de.glmtk.querying.estimator.Estimators.DIFF_INTERPOL_ABS_THREE_DISCOUNT_MLE_SKP;
import static de.glmtk.querying.estimator.Estimators.FALSE_MLE;
import static de.glmtk.querying.estimator.Estimators.INTERPOL_ABS_DISCOUNT_MLE_DEL;
import static de.glmtk.querying.estimator.Estimators.INTERPOL_ABS_DISCOUNT_MLE_DEL_NOREC;
import static de.glmtk.querying.estimator.Estimators.INTERPOL_ABS_DISCOUNT_MLE_SKP;
import static de.glmtk.querying.estimator.Estimators.INTERPOL_ABS_DISCOUNT_MLE_SKP_NOREC;
import static de.glmtk.querying.estimator.Estimators.INTERPOL_ABS_THREE_DISCOUNT_MLE_DEL;
import static de.glmtk.querying.estimator.Estimators.MLE;
import static de.glmtk.querying.estimator.Estimators.UNIFORM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.glmtk.Glmtk;
import de.glmtk.cache.OldCache;
import de.glmtk.cache.CacheBuilder;
import de.glmtk.common.NGram;
import de.glmtk.common.ProbMode;
import de.glmtk.exceptions.SwitchCaseNotImplementedException;
import de.glmtk.logging.Logger;
import de.glmtk.querying.calculator.Calculator;
import de.glmtk.querying.calculator.SequenceCalculator;
import de.glmtk.querying.estimator.Estimator;
import de.glmtk.testutil.EstimatorTestRunner;
import de.glmtk.testutil.EstimatorTestRunner.EstimatorTestParameters;
import de.glmtk.testutil.EstimatorTestRunner.EstimatorTestParams;
import de.glmtk.testutil.EstimatorTestRunner.IgnoreProbMode;
import de.glmtk.testutil.TestCorporaTest;
import de.glmtk.testutil.TestCorpus;

@RunWith(EstimatorTestRunner.class)
@IgnoreProbMode(ProbMode.COND)
public class EstimatorTest extends TestCorporaTest {
    private static final Logger LOGGER = Logger.get(EstimatorTest.class);
    private static final List<TestCorpus> TEST_CORPORA = Arrays.asList(
            TestCorpus.ABC, TestCorpus.MOBYDICK);
    private static final int HIGHEST_ORDER = 5;

    private static boolean countsAvailable = false;

    @EstimatorTestParameters
    public static Iterable<EstimatorTestParams> data() {
        //@formatter:off
        return Arrays.asList(
                // Substitute Estimators
                new EstimatorTestParams(UNIFORM, false, 0, HIGHEST_ORDER),
                new EstimatorTestParams(ABS_UNIGRAM, false, 0, HIGHEST_ORDER),
                new EstimatorTestParams(CONT_UNIGRAM, false, 0, HIGHEST_ORDER - 1),

                // Fractions Estimators
                new EstimatorTestParams(MLE, false, HIGHEST_ORDER, HIGHEST_ORDER),
                new EstimatorTestParams(FALSE_MLE, false, 0, HIGHEST_ORDER),
                new EstimatorTestParams(CMLE, true, HIGHEST_ORDER - 1, HIGHEST_ORDER - 1),

                // Backoff Estimators
                new EstimatorTestParams(BACKOFF_CMLE_NOREC, true, HIGHEST_ORDER - 1, HIGHEST_ORDER - 1),
                new EstimatorTestParams(BACKOFF_CMLE, true, HIGHEST_ORDER - 1, HIGHEST_ORDER - 1),

                // Interpol Estimators
                new EstimatorTestParams(INTERPOL_ABS_DISCOUNT_MLE_SKP_NOREC, false, HIGHEST_ORDER, HIGHEST_ORDER),
                new EstimatorTestParams(INTERPOL_ABS_DISCOUNT_MLE_DEL_NOREC, false, HIGHEST_ORDER, HIGHEST_ORDER),
                new EstimatorTestParams(INTERPOL_ABS_DISCOUNT_MLE_SKP, false, HIGHEST_ORDER, HIGHEST_ORDER),
                new EstimatorTestParams(INTERPOL_ABS_DISCOUNT_MLE_DEL, false, HIGHEST_ORDER, HIGHEST_ORDER),

                // DiffInterpol Estimators
                new EstimatorTestParams(DIFF_INTERPOL_ABS_DISCOUNT_MLE_SKP_NOREC, false, HIGHEST_ORDER, HIGHEST_ORDER),
                new EstimatorTestParams(DIFF_INTERPOL_ABS_DISCOUNT_MLE_DEL_NOREC, false, HIGHEST_ORDER, HIGHEST_ORDER),
                new EstimatorTestParams(DIFF_INTERPOL_ABS_DISCOUNT_MLE_DEL_FRONT_NOREC, false, HIGHEST_ORDER, HIGHEST_ORDER),
                // HIGHEST_ORDER should actually also work, but takes far to long to calculate
                new EstimatorTestParams(DIFF_INTERPOL_ABS_DISCOUNT_MLE_SKP_AND_DEL_NOREC, false, HIGHEST_ORDER - 1, HIGHEST_ORDER - 1),
                new EstimatorTestParams(DIFF_INTERPOL_ABS_DISCOUNT_MLE_SKP, false, HIGHEST_ORDER, HIGHEST_ORDER),
                new EstimatorTestParams(DIFF_INTERPOL_ABS_DISCOUNT_MLE_DEL, false, HIGHEST_ORDER, HIGHEST_ORDER),
                new EstimatorTestParams(DIFF_INTERPOL_ABS_DISCOUNT_MLE_DEL_FRONT, false, HIGHEST_ORDER, HIGHEST_ORDER),
                // HIGHEST_ORDER should actually also work, but takes far to long to calculate.
                new EstimatorTestParams(DIFF_INTERPOL_ABS_DISCOUNT_MLE_SKP_AND_DEL, false, HIGHEST_ORDER - 1, HIGHEST_ORDER - 1),

                new EstimatorTestParams(INTERPOL_ABS_THREE_DISCOUNT_MLE_DEL, false, HIGHEST_ORDER, HIGHEST_ORDER),
                new EstimatorTestParams(DIFF_INTERPOL_ABS_THREE_DISCOUNT_MLE_SKP, false, HIGHEST_ORDER, HIGHEST_ORDER),

                // Combination Estimators
                new EstimatorTestParams(COMB_MLE_CMLE, true, 0, HIGHEST_ORDER - 1)
                );
        //@formatter:on
    }

    @BeforeClass
    public static void setUpCountsAvailable() throws Exception {
        if (countsAvailable)
            return;

        CacheBuilder requiredCache = new CacheBuilder();
        for (EstimatorTestParams params : data()) {
            int highestOrder = Math.max(params.getCondOrder(),
                    params.getMargOrder());
            requiredCache.addAll(params.getEstimator().getRequiredCache(
                    highestOrder));
        }

        for (TestCorpus testCorpus : TEST_CORPORA) {
            Glmtk glmtk = testCorpus.getGlmtk();
            glmtk.count(requiredCache.getCountsPatterns());
        }

        countsAvailable = true;
    }

    private Estimator estimator;
    private boolean continuationEstimator;
    private ProbMode probMode;
    private int maxOrder;

    public EstimatorTest(Estimator estimator,
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
    public void testNGramProbabilitiesSum() throws Exception {
        LOGGER.info("=== Test NGram Probabilities Sum");
        Calculator calculator = new SequenceCalculator();
        calculator.setEstimator(estimator);
        for (TestCorpus testCorpus : TEST_CORPORA) {
            LOGGER.info("# %s corpus", testCorpus.getCorpusName());

            OldCache cache = createCache(testCorpus);
            estimator.setCache(cache);

            for (int order = 1; order != maxOrder + 1; ++order) {
                double sum = 0;
                for (int i = 0; i != (int) Math.pow(
                        testCorpus.getTokens().length, order); ++i) {
                    List<String> sequence = testCorpus.getSequenceList(i, order);
                    double prob = calculator.probability(sequence);
                    if (prob < 0.0 && prob > 1.0)
                        fail("Illegal probability: " + prob);
                    sum += prob;
                }
                try {
                    assertEquals(1.0, sum, 1e-6);
                } catch (AssertionError e) {
                    LOGGER.error("n=%s: sum = %s fail", order, sum);
                    throw e;
                }
                LOGGER.info("n=%s: sum = %s", order, sum);
            }
        }
    }

    @Test
    public void testFixedHistorySum() throws Exception {
        LOGGER.info("=== Test Fixed History Sum");

        for (TestCorpus testCorpus : TEST_CORPORA) {
            LOGGER.info("# %s corpus", testCorpus.getCorpusName());

            OldCache cache = createCache(testCorpus);
            estimator.setCache(cache);

            for (int order = 1; order != maxOrder + 1; ++order) {
                LOGGER.info("n=%s", order);
                for (int i = 0; i != (int) Math.pow(
                        testCorpus.getTokens().length, order - 1); ++i) {
                    NGram history = new NGram(testCorpus.getSequenceList(i,
                            order - 1));

                    double sum = 0;
                    for (int j = 0; j != testCorpus.getTokens().length; ++j) {
                        NGram sequence = new NGram(
                                Arrays.asList(testCorpus.getTokens()[j]));
                        double prob = estimator.probability(sequence, history);
                        if (prob < 0.0 && prob > 1.0)
                            fail("Illegal probability: " + prob);
                        sum += prob;
                    }

                    try {
                        switch (probMode) {
                            case COND:
                                NGram checkHistory = history.concat(SKP_NGRAM);
                                if (continuationEstimator)
                                    checkHistory = SKP_NGRAM.concat(checkHistory);
                                if (checkHistory.seen(cache))
                                    assertEquals(1.0, sum, 1e-6);
                                else
                                    assertEquals(0.0, sum, 1e-6);
                                break;
                            case MARG:
                                assertEquals(1.0, sum, 1e-6);
                                break;
                            default:
                                throw new SwitchCaseNotImplementedException();
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

    private OldCache createCache(TestCorpus testCorpus) throws Exception {
        Glmtk glmtk = testCorpus.getGlmtk();

        CacheBuilder requiredCache = estimator.getRequiredCache(5);
        return requiredCache.build(glmtk.getPaths());
    }
}
