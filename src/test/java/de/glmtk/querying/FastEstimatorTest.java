package de.glmtk.querying;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import de.glmtk.Constants;
import de.glmtk.Glmtk;
import de.glmtk.common.CountCache;
import de.glmtk.common.Pattern;
import de.glmtk.common.Patterns;
import de.glmtk.common.ProbMode;
import de.glmtk.querying.estimator.Estimator;
import de.glmtk.querying.estimator.Estimators;
import de.glmtk.querying.estimator.fast.FastModifiedKneserNeyEstimator;
import de.glmtk.testutil.TestCorporaTest;
import de.glmtk.testutil.TestCorpus;

public class FastEstimatorTest extends TestCorporaTest {
    private static final Logger LOGGER = LogManager.getFormatterLogger(FastEstimatorTest.class);

    @Test
    public void testFastModifiedKneserEstimator() throws Exception {
        Estimator slowEstimator = Estimators.MOD_KNESER_NEY;
        FastModifiedKneserNeyEstimator fastEstimator = new FastModifiedKneserNeyEstimator();
        fastEstimator.setName("Fast-Modified-Kneser-Ney");

        Set<Pattern> patternsSlow = Patterns.getUsedPatterns(5, slowEstimator,
                ProbMode.MARG);
        Set<Pattern> patternsFast = Patterns.getUsedPatterns(5, fastEstimator,
                ProbMode.MARG);
        Set<Pattern> patterns = new HashSet<>();
        patterns.addAll(patternsSlow);
        patterns.addAll(patternsFast);

        LOGGER.debug("patternsSlow = %s", patternsSlow);
        LOGGER.debug("patternsFast = %s", patternsFast);
        LOGGER.debug("patterns     = %s", patterns);

        runEstimator(slowEstimator, patterns);
        runEstimator(fastEstimator, patterns);
    }

    private void runEstimator(Estimator estimator,
                              Set<Pattern> patterns) throws Exception {
        TestCorpus testCorpus = TestCorpus.EN0008T;

        QueryMode queryMode = QueryMode.newSequence();
        Path inputFile = Constants.TEST_RESSOURCES_DIR.resolve("en0008t.testing.5");
        CountCache countCache = testCorpus.getCountCache(patterns);
        ProbMode probMode = ProbMode.MARG;
        int corpusOrder = 5;

        long t = System.currentTimeMillis();
        Glmtk glmtk = testCorpus.getGlmtk();
        glmtk.runQueriesOnFile(queryMode, inputFile, estimator, probMode,
                countCache, corpusOrder);
        long tt = System.currentTimeMillis();
        System.out.println((tt - t) + "ms");
    }
}
