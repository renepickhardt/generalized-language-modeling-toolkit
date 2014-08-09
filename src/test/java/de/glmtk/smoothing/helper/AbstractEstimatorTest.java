package de.glmtk.smoothing.helper;

import static de.glmtk.smoothing.estimator.Estimators.ABS_UNIGRAM;
import static de.glmtk.smoothing.estimator.Estimators.CMLE;
import static de.glmtk.smoothing.estimator.Estimators.COMB_MLE_CMLE;
import static de.glmtk.smoothing.estimator.Estimators.CONT_UNIGRAM;
import static de.glmtk.smoothing.estimator.Estimators.FMLE;
import static de.glmtk.smoothing.estimator.Estimators.INTERPOL_ABS_DISCOUNT_MLE;
import static de.glmtk.smoothing.estimator.Estimators.INTERPOL_ABS_DISCOUNT_MLE_REC;
import static de.glmtk.smoothing.estimator.Estimators.MLE;
import static de.glmtk.smoothing.estimator.Estimators.UNIFORM;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;

import de.glmtk.smoothing.Corpus;
import de.glmtk.smoothing.ProbMode;
import de.glmtk.smoothing.estimator.Estimator;

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

    protected static TestCorpus abcTestCorpus;

    protected static Corpus abcCorpus;

    protected static TestCorpus mobyDickTestCorpus;

    protected static Corpus mobyDickCorpus;

    @BeforeClass
    public static void setUpCorpora() throws IOException, InterruptedException {
        abcTestCorpus = new AbcTestCorpus();
        abcCorpus = abcTestCorpus.getCorpus();
        mobyDickTestCorpus = new MobyDickTestCorpus();
        mobyDickCorpus = mobyDickTestCorpus.getCorpus();
    }

    // Substitute Estimators

    @Test
    public void testUniform() {
        testEstimator("Uniform", UNIFORM, probModeOnlyMarg, HIGHEST_TEST_ORDER,
                false);
    }

    @Test
    public void testAbsUnigram() {
        testEstimator("AbsUnigram", ABS_UNIGRAM, probModeOnlyMarg,
                HIGHEST_TEST_ORDER, false);
    }

    @Test
    public void testContUnigram() {
        testEstimator("ContUnigram", CONT_UNIGRAM, probModeOnlyMarg,
                HIGHEST_TEST_ORDER - 1, false);
    }

    // Fractions Estimators

    @Test
    public void testMLE() {
        testEstimator("MLE", MLE, probModeAll, HIGHEST_TEST_ORDER, false);
    }

    @Test
    public void testFMLE() {
        testEstimator("FMLE", FMLE, probModeOnlyMarg, HIGHEST_TEST_ORDER, false);
    }

    @Test
    public void testCMLE() {
        testEstimator("CMLE", CMLE, probModeAll, HIGHEST_TEST_ORDER - 1, true);
    }

    // Interpolation Estimators

    @Test
    public void testInterpolAbsDiscountMle() {
        testEstimator("InterpolAbsDiscountMle", INTERPOL_ABS_DISCOUNT_MLE,
                probModeAll, HIGHEST_TEST_ORDER, false);
    }

    @Test
    public void testInterpolAbsDiscountMleRec() {
        testEstimator("InterpolAbsDiscountMleRec",
                INTERPOL_ABS_DISCOUNT_MLE_REC, probModeAll, HIGHEST_TEST_ORDER,
                false);
    }

    // Combination Estimators

    @Test
    public void testCombMleCmle() {
        testEstimator("CombMleCmle", COMB_MLE_CMLE, probModeAll,
                HIGHEST_TEST_ORDER - 1, true);
    }

    protected abstract void testEstimator(
            String estimatorName,
            Estimator estimator,
            ProbMode[] probModes,
            int maxOrder,
            boolean continuationEstimator);

}
