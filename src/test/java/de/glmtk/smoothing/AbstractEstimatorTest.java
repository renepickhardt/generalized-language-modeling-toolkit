package de.glmtk.smoothing;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import de.glmtk.smoothing.estimating.Estimator;
import de.glmtk.smoothing.estimating.Estimators;

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
    public void testMLE() {
        testEstimator("MLE", Estimators.MLE, HIGHEST_TEST_ORDER);
    }

    @Test
    public void testCMLE() {
        testEstimator("CMLE", Estimators.CMLE, HIGHEST_TEST_ORDER - 1);
    }

    @Ignore
    @Test
    public void testFMLE() {
        testEstimator("FMLE", Estimators.FMLE, HIGHEST_TEST_ORDER);
    }

    @Test
    public void testInterpolAbsDiscountMle() {
        testEstimator("InterpolAbsDiscountMle",
                Estimators.INTERPOL_ABS_DISCOUNT_MLE, HIGHEST_TEST_ORDER);
    }

    protected abstract void testEstimator(
            String testName,
            Estimator estimator,
            int maxOrder);

}
