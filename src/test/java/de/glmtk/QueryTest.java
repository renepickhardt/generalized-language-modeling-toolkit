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
        TestCorpus testCorpus = TestCorpus.ABC;
        Estimator estimator = Estimators.DIFF_INTERPOL_ABS_DISCOUNT_MLE_DEL_NOREC;
        CacheBuilder requiredCache = estimator.getRequiredCache(5);
        Cache cache = requiredCache.build(testCorpus.getGlmtk().getPaths());
        estimator.setCache(cache);

        estimator.probability(new NGram("a"), new NGram(StringUtils.split(
                "a c", ' ')));
    }
}
