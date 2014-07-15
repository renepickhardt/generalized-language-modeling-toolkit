package de.typology.smoothing;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class SumEquals1Test extends LoggingTest {

    private static Logger logger = LogManager.getLogger(SumEquals1Test.class);

    private static final int HIGHEST_TEST_ORDER = 5;

    private static final double ABS_INTERPOL_LAMBDA = .5;

    private static TestCorpus abcTestCorpus;

    private static Corpus abcCorpus;

    private static TestCorpus mobyDickTestCorpus;

    private static Corpus mobyDickCorpus;

    @BeforeClass
    public static void setUpBeforeClass() throws IOException,
            InterruptedException {
        abcTestCorpus = new AbcTestCorpus();
        abcCorpus = abcTestCorpus.getCorpus();
        mobyDickTestCorpus = new MobyDickTestCorpus();
        mobyDickCorpus = mobyDickTestCorpus.getCorpus();
    }

    @Test
    public void testMle() {
        assertEstimatorSumEquals1("MaximumLikelihoodEstimator",
                new MaximumLikelihoodEstimator(), HIGHEST_TEST_ORDER + 1);
    }

    @Test
    public void testCmle() {
        assertEstimatorSumEquals1("ContinuationMaximumLikelihoodEstimator",
                new ContinuationMaximumLikelihoodEstimator(),
                HIGHEST_TEST_ORDER);
    }

    @Test
    public void testBackoffMle() {
        assertEstimatorSumEquals1("BackoffEstimator(MLE, MLE)",
                new BackoffEstimator(new MaximumLikelihoodEstimator(),
                        new MaximumLikelihoodEstimator()),
                HIGHEST_TEST_ORDER + 1);
    }

    @Test
    public void testBackoffMleRec() {
        assertEstimatorSumEquals1("recursive BackoffEstimator(MLE)",
                new BackoffEstimator(new MaximumLikelihoodEstimator()),
                HIGHEST_TEST_ORDER + 1);
    }

    @Test
    public void testAbsInterpolMleCmle() {
        assertEstimatorSumEquals1("AbsoluteInterpolEstimator(MLE, CMLE)",
                new AbsoluteInterpolEstimator(new MaximumLikelihoodEstimator(),
                        new ContinuationMaximumLikelihoodEstimator(),
                        ABS_INTERPOL_LAMBDA), HIGHEST_TEST_ORDER);
    }

    @Test
    public void testAbsInterpolMleRec() {
        assertEstimatorSumEquals1("recursive AbsoluteInterpolEstimator(MLE)",
                new AbsoluteInterpolEstimator(new MaximumLikelihoodEstimator(),
                        ABS_INTERPOL_LAMBDA), HIGHEST_TEST_ORDER + 1);
    }

    private void assertEstimatorSumEquals1(
            String testName,
            Estimator estimator,
            int maxOrder) {
        logger.info("===== {} =====", testName);
        assertCalculatorSumEquals1(estimator, new SkipCalculator(estimator),
                maxOrder);
        assertCalculatorSumEquals1(estimator, new DeleteCalculator(estimator),
                maxOrder);
    }

    private void assertCalculatorSumEquals1(
            Estimator estimator,
            PropabilityCalculator calculator,
            int maxOrder) {
        logger.info("=== {}", calculator.getClass().getSimpleName());
        assertCorpusSumEquals1(estimator, calculator, abcCorpus, abcTestCorpus,
                maxOrder);
        assertCorpusSumEquals1(estimator, calculator, mobyDickCorpus,
                mobyDickTestCorpus, maxOrder);
    }

    private void assertCorpusSumEquals1(
            Estimator estimator,
            PropabilityCalculator calculator,
            Corpus corpus,
            TestCorpus testCorpus,
            int maxOrder) {
        logger.info("# {} corpus", testCorpus.getWorkingDir().getFileName());

        estimator.setCorpus(corpus);
        for (int order = 1; order != maxOrder; ++order) {
            double sum = 0;
            for (int i = 0; i != (int) Math.pow(testCorpus.getWords().length,
                    order); ++i) {
                String sequence = testCorpus.getSequence(i, order);
                sum += calculator.propability(sequence);
            }
            logger.info("n={}: sum = {}", order, sum);
            Assert.assertEquals(1, sum, 0.01);
        }
        logger.info("passed");
    }

}
