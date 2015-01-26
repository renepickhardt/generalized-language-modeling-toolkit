package de.glmtk.querying;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import de.glmtk.Constants;
import de.glmtk.Glmtk;
import de.glmtk.GlmtkPaths;
import de.glmtk.cache.Cache;
import de.glmtk.cache.CacheBuilder;
import de.glmtk.logging.Logger;
import de.glmtk.querying.estimator.Estimator;
import de.glmtk.querying.estimator.Estimators;
import de.glmtk.querying.estimator.fast.FastModKneserNeyEstimator;
import de.glmtk.querying.estimator.learned.LearnedModKneserNeyEstimator;
import de.glmtk.testutil.TestCorporaTest;
import de.glmtk.testutil.TestCorpus;

@RunWith(Parameterized.class)
public class EstimatorSpeedTest extends TestCorporaTest {
    private static class EstimatorSpeed {
        private long cacheLoadingTime;
        private long queryingTime;

        public EstimatorSpeed(long cacheLoadingTime,
                              long queryingTime) {
            super();
            this.cacheLoadingTime = cacheLoadingTime;
            this.queryingTime = queryingTime;
        }

        public long getCacheLoadingTime() {
            return cacheLoadingTime;
        }

        public long getQueryingTime() {
            return queryingTime;
        }
    }

    private static final Logger LOGGER = Logger.get(EstimatorSpeedTest.class);

    private static TestCorpus testCorpus = TestCorpus.EN0008T;
    private static Path testFile = Constants.TEST_RESSOURCES_DIR.resolve("en0008t.testing.5.100");

    private static Map<Estimator, EstimatorSpeed> results = new LinkedHashMap<>();

    @Parameters(name = "{0}")
    public static Iterable<Object[]> data() {
        Estimator fastMkn = new FastModKneserNeyEstimator();
        fastMkn.setName("Fast-Modified-Kneser-Ney");
        Estimator learnedMkn = new LearnedModKneserNeyEstimator();
        learnedMkn.setName("Learned-Modified-Kneser-Ney");

        return Arrays.asList(new Object[][] { {Estimators.MOD_KNESER_NEY},
                {fastMkn}, {learnedMkn}});
    }

    @AfterClass
    public static void displayResults() {
        int maxNameLength = 0;
        for (Estimator estimator : results.keySet())
            if (maxNameLength < estimator.toString().length())
                maxNameLength = estimator.toString().length();

        for (Entry<Estimator, EstimatorSpeed> entry : results.entrySet()) {
            Estimator estimator = entry.getKey();
            EstimatorSpeed estimatorSpeed = entry.getValue();

            LOGGER.info("%-" + maxNameLength
                    + "s Estimator:  Loading Cache: %6dms  Querying: %6dms",
                    estimator, estimatorSpeed.getCacheLoadingTime(),
                    estimatorSpeed.getQueryingTime());
        }
    }

    private Estimator estimator;

    public EstimatorSpeedTest(Estimator estimator) {
        this.estimator = estimator;
    }

    @Test
    public void testSpeed() throws Exception {
        Glmtk glmtk = testCorpus.getGlmtk();
        GlmtkPaths paths = glmtk.getPaths();

        CacheBuilder requiredCache = estimator.getRequiredCache(5);

        long timeBeforeCacheBuild = System.currentTimeMillis();
        Cache cache = requiredCache.build(paths);
        long timeAfterCacheBuild = System.currentTimeMillis();

        estimator.setCache(cache);

        Files.createDirectories(paths.getQueriesDir());
        Path outputFile = paths.getQueriesDir().resolve(
                testFile.getFileName() + " " + estimator.toString());

        long timeBeforeQuerying = System.currentTimeMillis();
        glmtk.queryFile(QueryMode.newSequence(), estimator, 5, testFile,
                outputFile);
        long timeAfterQuerying = System.currentTimeMillis();

        results.put(estimator, new EstimatorSpeed(timeAfterCacheBuild
                - timeBeforeCacheBuild, timeAfterQuerying - timeBeforeQuerying));
    }
}
