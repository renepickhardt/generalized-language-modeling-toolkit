package de.glmtk.smoothing;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

public abstract class AbstractEstimatorTest extends LoggingTest {

    protected static Logger logger = LogManager
            .getLogger(AbstractEstimatorTest.class);

    protected static final int HIGHEST_TEST_ORDER = 5;

    protected static final double ABS_INTERPOL_LAMBDA = .75;

    protected static final double ABS_DISCOUNT = .5;

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
    public void testMle() {
        testEstimator("MaximumLikelihoodEstimator",
                new MaximumLikelihoodEstimator(), HIGHEST_TEST_ORDER + 1);
    }

    @Test
    public void testCmle() {
        testEstimator("ContinuationMaximumLikelihoodEstimator",
                new ContinuationMaximumLikelihoodEstimator(),
                HIGHEST_TEST_ORDER);
    }

    @Test
    public void testBackoffMle() {
        testEstimator("BackoffEstimator(MLE, MLE)", new BackoffEstimator(
                new MaximumLikelihoodEstimator(),
                new MaximumLikelihoodEstimator()), HIGHEST_TEST_ORDER + 1);
    }

    @Test
    public void testBackoffMleRec() {
        testEstimator("recursive BackoffEstimator(MLE)", new BackoffEstimator(
                new MaximumLikelihoodEstimator()), HIGHEST_TEST_ORDER + 1);
    }

    @Test
    public void testAbsInterpolMleCmle() {
        testEstimator("AbsoluteInterpolEstimator(MLE, CMLE)",
                new AbsoluteInterpolEstimator(new MaximumLikelihoodEstimator(),
                        new ContinuationMaximumLikelihoodEstimator(),
                        ABS_INTERPOL_LAMBDA), HIGHEST_TEST_ORDER);
    }

    @Test
    public void testAbsInterpolMleRec() {
        testEstimator("recursive AbsoluteInterpolEstimator(MLE)",
                new AbsoluteInterpolEstimator(new MaximumLikelihoodEstimator(),
                        ABS_INTERPOL_LAMBDA), HIGHEST_TEST_ORDER + 1);
    }

    @Test
    public void testBackoffAbsDiscountMle() {
        testEstimator("BackoffAbsoluteDiscountEstimator(MLE)",
                new BackoffEstimator(new AbsoluteDiscountEstimator(
                        new MaximumLikelihoodEstimator(), ABS_DISCOUNT),
                        new MaximumLikelihoodEstimator()),
                HIGHEST_TEST_ORDER + 1);
    }

    @Test
    public void testKatzEstimator() {
        testEstimator("KatzEstimator", new KatzEstimator(),
                HIGHEST_TEST_ORDER + 1);
    }

    @Test
    public void testTestEstimator() {
        testEstimator("TestEstimator", new TestEstimator(.75,
                new MaximumLikelihoodEstimator()), HIGHEST_TEST_ORDER + 1);
    }

    // add more estimators here

    protected abstract void testEstimator(
            String testName,
            Estimator estimator,
            int maxOrder);

}
