package de.glmtk;

import org.junit.Test;

import de.glmtk.cache.CacheBuilder;
import de.glmtk.cache.CacheBuilder.CacheImplementation;
import de.glmtk.cache.CompletionTrieCache;
import de.glmtk.common.Pattern;
import de.glmtk.querying.estimator.Estimators;
import de.glmtk.querying.estimator.weightedsum.WeightedSumModKneserNeyEstimator;
import de.glmtk.testutil.TestCorporaTest;
import de.glmtk.testutil.TestCorpus;
import de.glmtk.util.completiontrie.CompletionTrie;
import de.glmtk.util.completiontrie.CompletionTrieEntry;

/**
 * Playground to experiment with quick and dirty code ideas.
 */
public class QueryTest extends TestCorporaTest {
    @Test
    public void test() throws Exception {
        TestCorpus testCorpus = TestCorpus.EN0008T;
        WeightedSumModKneserNeyEstimator estimator = Estimators.WEIGHTEDSUM_MKN;
        CacheBuilder requiredCache = estimator.getRequiredCache(5).withWords().withCacheImplementation(
                CacheImplementation.COMPLETION_TRIE);
        CompletionTrieCache cache = (CompletionTrieCache) requiredCache.withProgress().build(
                testCorpus.getGlmtk().getPaths());

        estimator.setCache(cache);

        //        for (String word : cache.getWords())
        //            System.out.println(word);

        System.out.println("YOLO");

        CompletionTrie trie = cache.getCountCompletionTrie(Pattern.CNT_PATTERN);
        for (CompletionTrieEntry entry : trie)
            System.out.println(entry.getString());

        //        //@formatter:off
        //        List<String> list = Arrays.asList(
        //                "4 . 3 speak an",
        //                "4 . 3 speak",
        //                "4 . 3",
        //                "4 .",
        //                "4"
        //                );
        //        //@formatter:on
        //
        //        for (String string : list) {
        //            List<String> split = StringUtils.split(string, ' ');
        //            NGram sequence = new NGram(split.remove(split.size() - 1));
        //            NGram history = new NGram(split);
        //
        //            //            System.out.println("### " + history + " | " + sequence);
        //
        //            estimator.probability(sequence, history);
        //
        //            //            WeightedSumFunction weightedSumFunction = estimator.calcWeightedSumFunction(history);
        //            //            System.out.println(weightedSumFunction);
        //
        //            //            LOGGER.info("    %e ", estimator.probability(sequence,
        //            //                    weightedSumFunction));
        //        }
    }
}
