package de.glmtk.querying;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.AfterClass;
import org.junit.BeforeClass;
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
import de.glmtk.querying.estimator.fast.FastGeneralizedLanguageModelAbsEstimator;
import de.glmtk.querying.estimator.fast.FastGeneralizedLanguageModelEstimator;
import de.glmtk.querying.estimator.fast.FastModKneserNeyAbsEstimator;
import de.glmtk.querying.estimator.fast.FastModKneserNeyEstimator;
import de.glmtk.querying.estimator.learned.LearnedModKneserNeyEstimator;
import de.glmtk.testutil.TestCorporaTest;
import de.glmtk.testutil.TestCorpus;

@RunWith(Parameterized.class)
public class EstimatorSpeedTest extends TestCorporaTest {
    private static final Logger LOGGER = Logger.get(EstimatorSpeedTest.class);

    private static TestCorpus testCorpus = TestCorpus.EN0008T;
    private static Path testFile = Constants.TEST_RESSOURCES_DIR.resolve("en0008t.testing.5");

    private static List<Estimator> estimators;
    static {
        Estimator fastMknAbs = new FastModKneserNeyAbsEstimator();
        fastMknAbs.setName("Fast-Modified-Kneser-Ney (Abs-Lower-Order)");
        Estimator fastMkn = new FastModKneserNeyEstimator();
        fastMkn.setName("Fast-Modified-Kneser-Ney");
        Estimator learnedMkn = new LearnedModKneserNeyEstimator();
        learnedMkn.setName("Learned-Modified-Kneser-Ney");

        Estimator fastGlmAbs = new FastGeneralizedLanguageModelAbsEstimator();
        fastGlmAbs.setName("Fast-Generalized-Language-Model (Abs-Lower-Order)");
        Estimator fastGlm = new FastGeneralizedLanguageModelEstimator();
        fastGlm.setName("Fast-Generalized-Language-Model");

        //@formatter:off
        estimators = Arrays.asList(
                Estimators.MOD_KNESER_NEY_ABS,
                fastMknAbs,

                Estimators.MOD_KNESER_NEY,
                fastMkn,
                learnedMkn,

                Estimators.GLM_ABS,
                fastGlmAbs,

                Estimators.GLM,
                fastGlm
                );
        //@formatter:on
    }

    private static Map<Estimator, Long> results = new LinkedHashMap<>();

    private static Cache cache = null;

    @Parameters(name = "{0}")
    public static Iterable<Object[]> data() {
        List<Object[]> result = new ArrayList<>();
        for (Estimator estimator : estimators)
            result.add(new Object[] {estimator});
        return result;
    }

    @BeforeClass
    public static void loadCache() throws IOException {
        GlmtkPaths paths = testCorpus.getGlmtk().getPaths();

        CacheBuilder requiredCache = new CacheBuilder().withProgress();
        for (Estimator estimator : estimators)
            requiredCache.addAll(estimator.getRequiredCache(5));
        cache = requiredCache.build(paths);
    }

    @AfterClass
    public static void displayResults() {
        int maxNameLength = 0;
        for (Estimator estimator : results.keySet())
            if (maxNameLength < estimator.toString().length())
                maxNameLength = estimator.toString().length();

        for (Entry<Estimator, Long> entry : results.entrySet()) {
            Estimator estimator = entry.getKey();
            Long speed = entry.getValue();

            LOGGER.info("%-" + maxNameLength + "s Estimator:  Querying: %6dms",
                    estimator, speed);
        }
    }

    private Estimator estimator;

    public EstimatorSpeedTest(Estimator estimator) {
        this.estimator = estimator;
    }

    @Test
    public void testSpeed() throws Exception {
        Runtime.getRuntime().gc();

        Glmtk glmtk = testCorpus.getGlmtk();
        GlmtkPaths paths = glmtk.getPaths();

        estimator.setCache(cache);

        Files.createDirectories(paths.getQueriesDir());
        Path outputFile = paths.getQueriesDir().resolve(
                testFile.getFileName() + " " + estimator.toString());

        long timeBeforeQuerying = System.currentTimeMillis();
        glmtk.queryFile(QueryMode.newSequence(), estimator, 5, testFile,
                outputFile);
        long timeAfterQuerying = System.currentTimeMillis();

        results.put(estimator, timeAfterQuerying - timeBeforeQuerying);
    }
}
