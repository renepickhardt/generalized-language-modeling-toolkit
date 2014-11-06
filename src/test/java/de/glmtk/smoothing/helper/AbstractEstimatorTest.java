package de.glmtk.smoothing.helper;

import java.io.IOException;

import org.junit.Test;

import de.glmtk.smoothing.ProbMode;
import de.glmtk.smoothing.estimator.Estimator;
import de.glmtk.smoothing.estimator.Estimators;

public abstract class AbstractEstimatorTest extends LoggingTest {

    protected static final int HIGHEST_TEST_ORDER = 5;

    protected static final ProbMode[] probModeAll = {
        //ProbMode.COND, ProbMode.MARG
        ProbMode.MARG
    //, ProbMode.COND
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
    public void testUniform() throws IOException {
        testEstimator("Uniform", Estimators.UNIFORM, probModeOnlyMarg,
                HIGHEST_TEST_ORDER, false);
    }

    @Test
    public void testAbsUnigram() throws IOException {
        testEstimator("AbsUnigram", Estimators.ABS_UNIGRAM, probModeOnlyMarg,
                HIGHEST_TEST_ORDER, false);
    }

    @Test
    public void testContUnigram() throws IOException {
        testEstimator("ContUnigram", Estimators.CONT_UNIGRAM, probModeOnlyMarg,
                HIGHEST_TEST_ORDER - 1, false);
    }

    // Fractions Estimators

    @Test
    public void testMLE() throws IOException {
        testEstimator("MLE", Estimators.MLE, probModeAll, HIGHEST_TEST_ORDER,
                false);
    }

    @Test
    public void testFMLE() throws IOException {
        testEstimator("FMLE", Estimators.FMLE, probModeOnlyMarg,
                HIGHEST_TEST_ORDER, false);
    }

    @Test
    public void testCMLE() throws IOException {
        testEstimator("CMLE", Estimators.CMLE, probModeAll,
                HIGHEST_TEST_ORDER - 1, true);
    }

    // Backoff Estimators

    @Test
    public void testBackoffCmle() throws IOException {
        testEstimator("BackoffCmle", Estimators.BACKOFF_CMLE, probModeAll,
                HIGHEST_TEST_ORDER - 1, true);
    }

    @Test
    public void testBackoffCmleRec() throws IOException {
        testEstimator("BackoffCmleRec", Estimators.BACKOFF_CMLE_REC,
                probModeAll, HIGHEST_TEST_ORDER - 1, true);
    }

    // Interpolation Estimators

    @Test
    public void testInterpolAbsDiscountMle() throws IOException {
        testEstimator("InterpolAbsDiscountMle",
                Estimators.INTERPOL_ABS_DISCOUNT_MLE, probModeAll,
                HIGHEST_TEST_ORDER, false);
    }

    @Test
    public void testInterpolAbsDiscountMleRec() throws IOException {
        testEstimator("InterpolAbsDiscountMleRec",
                Estimators.INTERPOL_ABS_DISCOUNT_MLE_REC, probModeAll,
                HIGHEST_TEST_ORDER, false);
    }

    // Combination Estimators

    @Test
    public void testCombMleCmle() throws IOException {
        testEstimator("CombMleCmle", Estimators.COMB_MLE_CMLE,
                probModeOnlyMarg, HIGHEST_TEST_ORDER - 1, true);
    }

    protected abstract void testEstimator(
            String estimatorName,
            Estimator estimator,
            ProbMode[] probModes,
            int maxOrder,
            boolean continuationEstimator) throws IOException;

}
