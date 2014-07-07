package de.typology.smoothing;

import java.io.IOException;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SumEquals1Test {

    private static Logger logger = LoggerFactory
            .getLogger(SumEquals1Test.class);

    private static int MAX_LENGTH = 5;

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
    public void testSkipMle() {
        logger.info("=== SkipMle ============================================");

        MaximumLikelihoodEstimator mle;
        SkipCalculator skipMle;

        logger.info("# Abc Corpus");

        mle = new MaximumLikelihoodEstimator(abcCorpus);
        skipMle = new SkipCalculator(mle);
        for (int i = 1; i != MAX_LENGTH + 1; ++i) {
            assertSumEquals1(skipMle, abcTestCorpus, i);
        }

        logger.info("# MobyDick Corpus");

        mle = new MaximumLikelihoodEstimator(mobyDickCorpus);
        skipMle = new SkipCalculator(mle);
        for (int i = 1; i != MAX_LENGTH + 1; ++i) {
            assertSumEquals1(skipMle, mobyDickTestCorpus, i);
        }

        logger.info("passed");
    }

    @Test
    public void testDeleteMle() {
        logger.info("=== DeleteMle ==========================================");

        MaximumLikelihoodEstimator mle;
        DeleteCalculator deleteMle;

        logger.info("# Abc Corpus");

        mle = new MaximumLikelihoodEstimator(abcCorpus);
        deleteMle = new DeleteCalculator(mle);
        for (int i = 1; i != MAX_LENGTH + 1; ++i) {
            assertSumEquals1(deleteMle, abcTestCorpus, i);
        }

        logger.info("# MobyDick Corpus");

        mle = new MaximumLikelihoodEstimator(mobyDickCorpus);
        deleteMle = new DeleteCalculator(mle);
        for (int i = 1; i != MAX_LENGTH + 1; ++i) {
            assertSumEquals1(deleteMle, mobyDickTestCorpus, i);
        }

        logger.info("passed");
    }

    @Test
    public void testSkipCmle() {
        logger.info("=== SkipCmle ===========================================");

        ContinuationMaximumLikelihoodEstimator cmle;
        SkipCalculator skipMle;

        logger.info("# Abc Corpus");

        cmle = new ContinuationMaximumLikelihoodEstimator(abcCorpus);
        skipMle = new SkipCalculator(cmle);
        for (int i = 1; i != MAX_LENGTH; ++i) {
            assertSumEquals1(skipMle, abcTestCorpus, i);
        }

        logger.info("# MobyDick Corpus");

        cmle = new ContinuationMaximumLikelihoodEstimator(mobyDickCorpus);
        skipMle = new SkipCalculator(cmle);
        for (int i = 1; i != MAX_LENGTH; ++i) {
            assertSumEquals1(skipMle, mobyDickTestCorpus, i);
        }

        logger.info("passed");
    }

    @Test
    public void testDeleteCmle() {
        logger.info("=== DeleteCmle =========================================");

        ContinuationMaximumLikelihoodEstimator cmle;
        DeleteCalculator deleteMle;

        logger.info("# Abc Corpus");

        cmle = new ContinuationMaximumLikelihoodEstimator(abcCorpus);
        deleteMle = new DeleteCalculator(cmle);
        for (int i = 1; i != MAX_LENGTH; ++i) {
            assertSumEquals1(deleteMle, abcTestCorpus, i);
        }

        logger.info("# MobyDick Corpus");

        cmle = new ContinuationMaximumLikelihoodEstimator(mobyDickCorpus);
        deleteMle = new DeleteCalculator(cmle);
        for (int i = 1; i != MAX_LENGTH; ++i) {
            assertSumEquals1(deleteMle, mobyDickTestCorpus, i);
        }

        logger.info("passed");
    }

    @Test
    public void testSkipBackoffMle() {
        logger.info("=== SkipBackoffMle =====================================");

        MaximumLikelihoodEstimator mle;
        BackoffEstimator backoffMle;
        SkipCalculator skipBackoffMle;

        logger.info("# Abc Corpus");

        mle = new MaximumLikelihoodEstimator(abcCorpus);
        backoffMle = new BackoffEstimator(abcCorpus, mle, mle);
        skipBackoffMle = new SkipCalculator(backoffMle);
        for (int i = 1; i != MAX_LENGTH + 1; ++i) {
            assertSumEquals1(skipBackoffMle, abcTestCorpus, i);
        }

        logger.info("# Moby Dick Corpus");

        mle = new MaximumLikelihoodEstimator(mobyDickCorpus);
        backoffMle = new BackoffEstimator(mobyDickCorpus, mle, mle);
        skipBackoffMle = new SkipCalculator(backoffMle);
        for (int i = 1; i != MAX_LENGTH + 1; ++i) {
            assertSumEquals1(skipBackoffMle, mobyDickTestCorpus, i);
        }

        logger.info("passed");
    }

    @Test
    public void testDeleteBackoffMle() {
        logger.info("=== DeleteBackoffMle ====================================");

        MaximumLikelihoodEstimator mle;
        BackoffEstimator backoffMle;
        DeleteCalculator skipBackoffMle;

        logger.info("# Abc Corpus");

        mle = new MaximumLikelihoodEstimator(abcCorpus);
        backoffMle = new BackoffEstimator(abcCorpus, mle, mle);
        skipBackoffMle = new DeleteCalculator(backoffMle);
        for (int i = 1; i != MAX_LENGTH + 1; ++i) {
            assertSumEquals1(skipBackoffMle, abcTestCorpus, i);
        }

        logger.info("# Moby Dick Corpus");

        mle = new MaximumLikelihoodEstimator(mobyDickCorpus);
        backoffMle = new BackoffEstimator(mobyDickCorpus, mle, mle);
        skipBackoffMle = new DeleteCalculator(backoffMle);
        for (int i = 1; i != MAX_LENGTH + 1; ++i) {
            assertSumEquals1(skipBackoffMle, mobyDickTestCorpus, i);
        }

        logger.info("passed");
    }

    @Test
    public void testSkipBackoffMleRec() {
        logger.info("=== SkipBackoffMleRec ==================================");

        MaximumLikelihoodEstimator mle;
        BackoffEstimator backoffMle;
        SkipCalculator skipBackoffMle;

        logger.info("# Abc Corpus");

        mle = new MaximumLikelihoodEstimator(abcCorpus);
        backoffMle = new BackoffEstimator(abcCorpus, mle);
        skipBackoffMle = new SkipCalculator(backoffMle);
        for (int i = 1; i != MAX_LENGTH + 1; ++i) {
            assertSumEquals1(skipBackoffMle, abcTestCorpus, i);
        }

        logger.info("# Moby Dick Corpus");

        mle = new MaximumLikelihoodEstimator(mobyDickCorpus);
        backoffMle = new BackoffEstimator(mobyDickCorpus, mle);
        skipBackoffMle = new SkipCalculator(backoffMle);
        for (int i = 1; i != MAX_LENGTH + 1; ++i) {
            assertSumEquals1(skipBackoffMle, mobyDickTestCorpus, i);
        }

        logger.info("passed");
    }

    @Test
    public void testDeleteBackoffMleRec() {
        logger.info("=== DeleteBackoffMleRec ================================");

        MaximumLikelihoodEstimator mle;
        BackoffEstimator backoffMle;
        DeleteCalculator skipBackoffMle;

        logger.info("# Abc Corpus");

        mle = new MaximumLikelihoodEstimator(abcCorpus);
        backoffMle = new BackoffEstimator(abcCorpus, mle);
        skipBackoffMle = new DeleteCalculator(backoffMle);
        for (int i = 1; i != MAX_LENGTH + 1; ++i) {
            assertSumEquals1(skipBackoffMle, abcTestCorpus, i);
        }

        logger.info("# Moby Dick Corpus");

        mle = new MaximumLikelihoodEstimator(mobyDickCorpus);
        backoffMle = new BackoffEstimator(mobyDickCorpus, mle);
        skipBackoffMle = new DeleteCalculator(backoffMle);
        for (int i = 1; i != MAX_LENGTH + 1; ++i) {
            assertSumEquals1(skipBackoffMle, mobyDickTestCorpus, i);
        }

        logger.info("passed");
    }

    private void assertSumEquals1(
            PropabilityCalculator calculator,
            TestCorpus testCorpus,
            int n) {
        double sum = 0;
        for (int i = 0; i != (int) Math.pow(testCorpus.getWords().length, n); ++i) {
            String sequence = testCorpus.getSequence(i, n);
            sum += calculator.propability(sequence);
        }
        logger.info("n=" + n + ": sum = " + sum);
        Assert.assertEquals(1, sum, 0.01);
    }
}
