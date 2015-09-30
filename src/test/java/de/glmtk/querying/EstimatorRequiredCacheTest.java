package de.glmtk.querying;

import java.util.Arrays;
import java.util.TreeSet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import de.glmtk.cache.CacheSpecification;
import de.glmtk.logging.Logger;
import de.glmtk.querying.estimator.Estimator;
import de.glmtk.querying.estimator.Estimators;
import de.glmtk.querying.estimator.fast.FastGenLangModelEstimator;
import de.glmtk.testutil.LoggingTest;


@RunWith(Parameterized.class)
public class EstimatorRequiredCacheTest extends LoggingTest {
    private static final Logger LOGGER =
        Logger.get(EstimatorRequiredCacheTest.class);

    @Parameters(name = "{0}")
    public static Iterable<Object[]> data() {
        Estimator fastGlm = new FastGenLangModelEstimator();
        fastGlm.setName("Fast-Generalized-Language-Model");

        //@formatter:off
        return Arrays.asList(new Object[][]{
                {Estimators.GLM},
                {Estimators.GLM_ABS},
                {fastGlm}
        });
        //@formatter:on
    }

    private Estimator estimator;

    public EstimatorRequiredCacheTest(Estimator estimator) {
        this.estimator = estimator;
    }

    @Test
    public void testRequiredCache() {
        CacheSpecification requiredCache = estimator.getRequiredCache(5);

        LOGGER.info("Estimator '%s' requires cache: %s", estimator,
            requiredCache);
        LOGGER.info(
            new TreeSet<>(requiredCache.getRequiredPatterns()).toString());
    }
}
