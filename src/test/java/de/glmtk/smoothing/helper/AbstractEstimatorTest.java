package de.glmtk.smoothing.helper;

import static de.glmtk.smoothing.estimator.Estimators.ABS_UNIGRAM;
import static de.glmtk.smoothing.estimator.Estimators.BACKOFF_CMLE;
import static de.glmtk.smoothing.estimator.Estimators.BACKOFF_CMLE_REC;
import static de.glmtk.smoothing.estimator.Estimators.CMLE;
import static de.glmtk.smoothing.estimator.Estimators.COMB_MLE_CMLE;
import static de.glmtk.smoothing.estimator.Estimators.CONT_UNIGRAM;
import static de.glmtk.smoothing.estimator.Estimators.DERIVE_INTERPOL_ABS_DISCOUNT_MLE;
import static de.glmtk.smoothing.estimator.Estimators.DERIVE_INTERPOL_ABS_DISCOUNT_MLE_REC;
import static de.glmtk.smoothing.estimator.Estimators.FMLE;
import static de.glmtk.smoothing.estimator.Estimators.INTERPOL_ABS_DISCOUNT_MLE;
import static de.glmtk.smoothing.estimator.Estimators.INTERPOL_ABS_DISCOUNT_MLE_REC;
import static de.glmtk.smoothing.estimator.Estimators.MLE;
import static de.glmtk.smoothing.estimator.Estimators.UNIFORM;

import java.io.IOException;

import org.junit.Test;

import de.glmtk.smoothing.ProbMode;
import de.glmtk.smoothing.estimator.Estimator;

public abstract class AbstractEstimatorTest extends TestCorporaTest {

    protected static final int HIGHEST_TEST_ORDER = 5;

    // Substitute Estimators

    @Test
    public void testUniformMarg() throws IOException {
        testEstimator("Uniform", UNIFORM, ProbMode.MARG, HIGHEST_TEST_ORDER,
                false);
    }

    @Test
    public void testAbsUnigramMarg() throws IOException {
        testEstimator("AbsUnigram", ABS_UNIGRAM, ProbMode.MARG,
                HIGHEST_TEST_ORDER, false);
    }

    @Test
    public void testContUnigramMarg() throws IOException {
        testEstimator("ContUnigram", CONT_UNIGRAM, ProbMode.MARG,
                HIGHEST_TEST_ORDER - 1, false);
    }

    // Fractions Estimators

    @Test
    public void testMleCond() throws IOException {
        testEstimator("MLE", MLE, ProbMode.COND, HIGHEST_TEST_ORDER, false);
    }

    @Test
    public void testMleMarg() throws IOException {
        testEstimator("MLE", MLE, ProbMode.MARG, HIGHEST_TEST_ORDER, false);
    }

    @Test
    public void testFmleCondMarg() throws IOException {
        testEstimator("FMLE", FMLE, ProbMode.MARG, HIGHEST_TEST_ORDER, false);
    }

    @Test
    public void testCmleCond() throws IOException {
        testEstimator("CMLE", CMLE, ProbMode.COND, HIGHEST_TEST_ORDER - 1, true);
    }

    @Test
    public void testCmleMarg() throws IOException {
        testEstimator("CMLE", CMLE, ProbMode.MARG, HIGHEST_TEST_ORDER - 1, true);
    }

    // Backoff Estimators

    @Test
    public void testBackoffCmleCond() throws IOException {
        testEstimator("BackoffCmle", BACKOFF_CMLE, ProbMode.COND,
                HIGHEST_TEST_ORDER - 1, true);
    }

    @Test
    public void testBackoffCmleMarg() throws IOException {
        testEstimator("BackoffCmle", BACKOFF_CMLE, ProbMode.MARG,
                HIGHEST_TEST_ORDER - 1, true);
    }

    @Test
    public void testBackoffCmleRecCond() throws IOException {
        testEstimator("BackoffCmleRec", BACKOFF_CMLE_REC, ProbMode.COND,
                HIGHEST_TEST_ORDER - 1, true);
    }

    @Test
    public void testBackoffCmleRecMarg() throws IOException {
        testEstimator("BackoffCmleRec", BACKOFF_CMLE_REC, ProbMode.MARG,
                HIGHEST_TEST_ORDER - 1, true);
    }

    // Interpolation Estimators

    @Test
    public void testInterpolAbsDiscountMleCond() throws IOException {
        testEstimator("InterpolAbsDiscountMle", INTERPOL_ABS_DISCOUNT_MLE,
                ProbMode.COND, HIGHEST_TEST_ORDER, false);
    }

    @Test
    public void testInterpolAbsDiscountMleMarg() throws IOException {
        testEstimator("InterpolAbsDiscountMle", INTERPOL_ABS_DISCOUNT_MLE,
                ProbMode.MARG, HIGHEST_TEST_ORDER, false);
    }

    @Test
    public void testInterpolAbsDiscountMleRecCond() throws IOException {
        testEstimator("InterpolAbsDiscountMleRec",
                INTERPOL_ABS_DISCOUNT_MLE_REC, ProbMode.COND,
                HIGHEST_TEST_ORDER, false);
    }

    @Test
    public void testInterpolAbsDiscountMleRecMarg() throws IOException {
        testEstimator("InterpolAbsDiscountMleRec",
                INTERPOL_ABS_DISCOUNT_MLE_REC, ProbMode.MARG,
                HIGHEST_TEST_ORDER, false);
    }

    @Test
    public void testDeriveInterpolAbsDiscountMleCond() throws IOException {
        testEstimator("DeriveInterpolAbsDiscountMle",
                DERIVE_INTERPOL_ABS_DISCOUNT_MLE, ProbMode.COND,
                HIGHEST_TEST_ORDER, false);
    }

    @Test
    public void testDeriveInterpolAbsDiscountMleMarg() throws IOException {
        testEstimator("DeriveInterpolAbsDiscountMle",
                DERIVE_INTERPOL_ABS_DISCOUNT_MLE, ProbMode.MARG,
                HIGHEST_TEST_ORDER, false);
    }

    @Test
    public void testDeriveInterpolAbsDiscountMleRecCond() throws IOException {
        testEstimator("DeriveInterpolAbsDiscountMle",
                DERIVE_INTERPOL_ABS_DISCOUNT_MLE_REC, ProbMode.COND,
                HIGHEST_TEST_ORDER, false);
    }

    @Test
    public void testDeriveInterpolAbsDiscountMleRecMarg() throws IOException {
        testEstimator("DeriveInterpolAbsDiscountMle",
                DERIVE_INTERPOL_ABS_DISCOUNT_MLE_REC, ProbMode.MARG,
                HIGHEST_TEST_ORDER, false);
    }

    // Combination Estimators

    @Test
    public void testCombMleCmleMarg() throws IOException {
        testEstimator("CombMleCmle", COMB_MLE_CMLE, ProbMode.MARG,
                HIGHEST_TEST_ORDER - 1, true);
    }

    protected abstract void testEstimator(
            String estimatorName,
            Estimator estimator,
            ProbMode probMode,
            int maxOrder,
            boolean continuationEstimator) throws IOException;

}
