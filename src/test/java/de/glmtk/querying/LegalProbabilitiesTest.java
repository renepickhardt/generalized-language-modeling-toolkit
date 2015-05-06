package de.glmtk.querying;

import java.io.IOException;
import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import de.glmtk.cache.Cache;
import de.glmtk.cache.CacheSpecification;
import de.glmtk.querying.estimator.Estimator;
import de.glmtk.querying.estimator.Estimators;
import de.glmtk.testutil.TestCorporaTest;
import de.glmtk.testutil.TestCorpus;

@RunWith(Parameterized.class)
public class LegalProbabilitiesTest extends TestCorporaTest {
    private static final TestCorpus TEST_CORPUS = TestCorpus.EN0008T;

    private static Cache cache;

    @BeforeClass
    public static void loadCache() throws IOException {
        CacheSpecification requiredCache = Estimators.GLM.getRequiredCache(5);
        cache = requiredCache.withProgress().build(
                TEST_CORPUS.getGlmtk().getPaths());
    }

    @Parameters
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {{Estimators.GLM}});
    }

    private Estimator estimator;

    public LegalProbabilitiesTest(Estimator estimator) {
        this.estimator = estimator;
    }

    @Test
    public void testLegalProbability() {
    }
}
