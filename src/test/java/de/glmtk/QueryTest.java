package de.glmtk;

import java.io.IOException;

import org.junit.Test;

import de.glmtk.cache.Cache;
import de.glmtk.cache.CacheBuilder;
import de.glmtk.common.NGram;
import de.glmtk.querying.estimator.Estimator;
import de.glmtk.querying.estimator.Estimators;
import de.glmtk.testutil.TestCorporaTest;
import de.glmtk.testutil.TestCorpus;
import de.glmtk.util.StringUtils;

/**
 * Playground to experiment with quick and dirty code ideas.
 */
public class QueryTest extends TestCorporaTest {
    @Test
    public void test() throws IOException {
        System.out.println(Runtime.getRuntime().totalMemory() / 1024 / 1024);
        TestCorpus testCorpus = TestCorpus.EN0008T;
        Estimator estimator = Estimators.GLM;
        CacheBuilder requiredCache = estimator.getRequiredCache(5);
        Cache cache = requiredCache.withProgress().build(
                testCorpus.getGlmtk().getPaths());
        estimator.setCache(cache);

        estimator.probability(new NGram("."), new NGram(StringUtils.split(
                "It is a", ' ')));
    }
}
