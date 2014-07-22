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
    public void testUniformEstimator() {
        testEstimator("UniformEstimator", Estimators.UNIFORM_ESTIMATOR,
                HIGHEST_TEST_ORDER);
    }

    @Test
    public void testAbsoluteUnigramEstimator() {
        testEstimator("AbsoluteUnigramEstimator",
                Estimators.ABSOLUTE_UNIGRAM_ESTIMATOR, HIGHEST_TEST_ORDER);
    }

    @Test
    public void testContinuationUnigramEstimator() {
        testEstimator("ContinuationUnigramEstimator",
                Estimators.CONTINUATION_UNIGRAM_ESTIMATOR,
                HIGHEST_TEST_ORDER - 1);
    }

    @Test
    public void testMaximumLikelihoodEstimator() {
        testEstimator("MaximumLikelihoodEstimator", Estimators.MLE,
                HIGHEST_TEST_ORDER);
    }

    @Test
    public void testContinuationMaximumLikelihoodEstimator() {
        testEstimator("ContinuationMaximumLikelihoodEstimator",
                Estimators.CMLE, HIGHEST_TEST_ORDER - 1);
    }

    @Ignore
    @Test
    public void testFalseMaximumLikelihoodEstimator() {
        testEstimator("FalseMaximumLikelihoodEstimator", Estimators.FMLE,
                HIGHEST_TEST_ORDER);
    }

    protected abstract void testEstimator(
            String testName,
            Estimator estimator,
            int maxOrder);

}
