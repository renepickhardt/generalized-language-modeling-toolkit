package de.glmtk;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import de.glmtk.cache.Cache;
import de.glmtk.cache.CacheBuilder;
import de.glmtk.common.NGram;
import de.glmtk.logging.Logger;
import de.glmtk.querying.estimator.Estimators;
import de.glmtk.querying.estimator.weightedsum.WeightedSumFunction;
import de.glmtk.querying.estimator.weightedsum.WeightedSumModKneserNeyEstimator;
import de.glmtk.testutil.TestCorporaTest;
import de.glmtk.testutil.TestCorpus;
import de.glmtk.util.StringUtils;

/**
 * Playground to experiment with quick and dirty code ideas.
 */
public class QueryTest extends TestCorporaTest {
    private static final Logger LOGGER = Logger.get(QueryTest.class);

    @Test
    public void test() throws Exception {
        TestCorpus testCorpus = TestCorpus.EN0008T;
        WeightedSumModKneserNeyEstimator estimator = Estimators.WEIGHTEDSUM_MKN;
        CacheBuilder requiredCache = estimator.getRequiredCache(5);
        Cache cache = requiredCache.withProgress().build(
                testCorpus.getGlmtk().provideQueryCache(
                        Constants.TEST_RESSOURCES_DIR.resolve("en0008t.testing.5"),
                        requiredCache.getCountsPatterns()));
        estimator.setCache(cache);

        //@formatter:off
        List<String> list = Arrays.asList(
                ". , New York ,",
                ". , New York",
                ". , New",
                ". ,",
                "."
                );
        //@formatter:on

        for (String string : list) {
            List<String> split = StringUtils.split(string, ' ');
            NGram sequence = new NGram(split.remove(split.size() - 1));
            NGram history = new NGram(split);

            //            System.out.println("### " + history + " | " + sequence);

            estimator.probability(sequence, history);

            WeightedSumFunction weightedSumFunction = estimator.calcWeightedSumFunction(history);
            //            System.out.println(weightedSumFunction);

            LOGGER.info("    %e ", estimator.probability(sequence,
                    weightedSumFunction));
        }
    }
}
