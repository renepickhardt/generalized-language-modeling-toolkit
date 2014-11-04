package de.glmtk.smoothing.helper;

import org.junit.Test;

import de.glmtk.smoothing.ProbMode;
import de.glmtk.smoothing.estimator.Estimator;
import de.glmtk.smoothing.estimator.Estimators;

public abstract class AbstractEstimatorTest extends LoggingTest {

    protected static final int HIGHEST_TEST_ORDER = 5;

    protected static final ProbMode[] probModeAll = {
        ProbMode.COND, ProbMode.MARG
    //ProbMode.MARG, ProbMode.COND
            };

    protected static final ProbMode[] probModeOnlyCond = {
        ProbMode.COND
    };

    protected static final ProbMode[] probModeOnlyMarg = {
        ProbMode.MARG
    };

    protected static final ProbMode[] probModeNone = {};

    // Substitute Estimators

    @Test
    public void testUniform() {
        testEstimator("Uniform", Estimators.UNIFORM, probModeOnlyMarg,
                HIGHEST_TEST_ORDER, false);
    }

    @Test
    public void testAbsUnigram() {
        testEstimator("AbsUnigram", Estimators.ABS_UNIGRAM, probModeOnlyMarg,
                HIGHEST_TEST_ORDER, false);
    }

    @Test
    public void testContUnigram() {
        testEstimator("ContUnigram", Estimators.CONT_UNIGRAM, probModeOnlyMarg,
                HIGHEST_TEST_ORDER - 1, false);
    }

    // Fractions Estimators

    @Test
    public void testMLE() {
        testEstimator("MLE", Estimators.MLE, probModeAll, HIGHEST_TEST_ORDER,
                false);
    }

    @Test
    public void testFMLE() {
        testEstimator("FMLE", Estimators.FMLE, probModeOnlyMarg,
                HIGHEST_TEST_ORDER, false);
    }

    @Test
    public void testCMLE() {
        testEstimator("CMLE", Estimators.CMLE, probModeAll,
                HIGHEST_TEST_ORDER - 1, true);
    }

    // Backoff Estimators

    @Test
    public void testBackoffCmle() {
        testEstimator("BackoffCmle", Estimators.BACKOFF_CMLE, probModeAll,
                HIGHEST_TEST_ORDER - 1, true);
    }

    @Test
    public void testBackoffCmleRec() {
        testEstimator("BackoffCmleRec", Estimators.BACKOFF_CMLE_REC,
                probModeAll, HIGHEST_TEST_ORDER - 1, true);
    }

    // Interpolation Estimators

    @Test
    public void testInterpolAbsDiscountMle() {
        testEstimator("InterpolAbsDiscountMle",
                Estimators.INTERPOL_ABS_DISCOUNT_MLE, probModeAll,
                HIGHEST_TEST_ORDER, false);
    }

    @Test
    public void testInterpolAbsDiscountMleRec() {
        testEstimator("InterpolAbsDiscountMleRec",
                Estimators.INTERPOL_ABS_DISCOUNT_MLE_REC, probModeAll,
                HIGHEST_TEST_ORDER, false);
    }

    // Combination Estimators

    @Test
    public void testCombMleCmle() {
        testEstimator("CombMleCmle", Estimators.COMB_MLE_CMLE,
                probModeOnlyMarg, HIGHEST_TEST_ORDER - 1, true);
    }

    protected abstract void testEstimator(
            String estimatorName,
            Estimator estimator,
            ProbMode[] probModes,
            int maxOrder,
            boolean continuationEstimator);

}
