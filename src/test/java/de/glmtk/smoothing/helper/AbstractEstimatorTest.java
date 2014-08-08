package de.glmtk.smoothing.helper;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;

import de.glmtk.smoothing.Corpus;
import de.glmtk.smoothing.ProbMode;
import de.glmtk.smoothing.estimator.Estimator;
import de.glmtk.smoothing.estimator.Estimators;

public abstract class AbstractEstimatorTest extends LoggingTest {

    protected static final int HIGHEST_TEST_ORDER = 5;

    protected static final ProbMode[] probModeAll = {
        ProbMode.COND, ProbMode.MARG
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

    protected abstract void testEstimator(
            String estimatorName,
            Estimator estimator,
            ProbMode[] probModes,
            int maxOrder,
            boolean continuationEstimator);

}
