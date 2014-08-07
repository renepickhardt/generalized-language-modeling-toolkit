package de.glmtk.smoothing.legacy3.helper;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import de.glmtk.smoothing.Corpus;
import de.glmtk.smoothing.helper.AbcTestCorpus;
import de.glmtk.smoothing.helper.LoggingTest;
import de.glmtk.smoothing.helper.MobyDickTestCorpus;
import de.glmtk.smoothing.helper.TestCorpus;
import de.glmtk.smoothing.legacy3.estimating.Estimator;
import de.glmtk.smoothing.legacy3.estimating.Estimators;

public abstract class AbstractEstimatorTest extends LoggingTest {

    protected static final int HIGHEST_TEST_ORDER = 5;

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

    @Test
    public void testUniform() {
        testEstimator("Uniform", Estimators.UNIFORM, HIGHEST_TEST_ORDER);
    }

    @Test
    public void testAbsoluteUnigram() {
        testEstimator("AbsoluteUnigram", Estimators.ABSOLUTE_UNIGRAM,
                HIGHEST_TEST_ORDER);
    }

    @Test
    public void testContinuationUnigram() {
        testEstimator("ContinuationUnigram", Estimators.CONTINUATION_UNIGRAM,
                HIGHEST_TEST_ORDER - 1);
    }

    @Test
    public void testMle() {
        testEstimator("Mle", Estimators.MLE, HIGHEST_TEST_ORDER);
    }

    @Test
    public void testCmle() {
        testEstimator("Cmle", Estimators.CMLE, HIGHEST_TEST_ORDER - 1);
    }

    @Ignore
    @Test
    public void testFmle() {
        testEstimator("Fmle", Estimators.FMLE, HIGHEST_TEST_ORDER);
    }

    @Test
    public void testInterpolAbsDiscountMle() {
        testEstimator("InterpolAbsDiscountMle",
                Estimators.INTERPOL_ABS_DISCOUNT_MLE, HIGHEST_TEST_ORDER);
    }

    @Test
    public void testBackoffCmle() {
        testEstimator("BackoffCmle", Estimators.BACKOFF_CMLE,
                HIGHEST_TEST_ORDER - 1);
    }

    @Ignore
    @Test
    public void testBackoffCmleRec() {
        testEstimator("BackoffCmleRec", Estimators.BACKOFF_CMLE_REC,
                HIGHEST_TEST_ORDER - 1);
    }

    protected abstract void testEstimator(
            String testName,
            Estimator estimator,
            int maxOrder);

}
