package de.glmtk;

import java.util.Iterator;

import org.junit.Test;

import de.glmtk.cache.CacheSpecification;
import de.glmtk.cache.CacheSpecification.CacheImplementation;
import de.glmtk.cache.CompletionTrieCache;
import de.glmtk.common.NGram;
import de.glmtk.common.Patterns;
import de.glmtk.logging.Log4jHelper;
import de.glmtk.logging.Logger.Level;
import de.glmtk.querying.estimator.Estimators;
import de.glmtk.querying.estimator.interpol.InterpolEstimator;
import de.glmtk.querying.estimator.weightedsum.WeightedSumFunction;
import de.glmtk.querying.estimator.weightedsum.WeightedSumModKneserNeyEstimator;
import de.glmtk.testutil.TestCorporaTest;
import de.glmtk.testutil.TestCorpus;
import de.glmtk.util.StringUtils;
import de.glmtk.util.completiontrie.CompletionTrie;
import de.glmtk.util.completiontrie.CompletionTrieEntry;

/**
 * Playground to experiment with quick and dirty code ideas.
 */
public class QueryTest extends TestCorporaTest {
    @Test
    public void test() throws Exception {
        Log4jHelper.setLogLevel(Level.TRACE);
        TestCorpus testCorpus = TestCorpus.EN0008T;
        InterpolEstimator glm = Estimators.GLM;
        WeightedSumModKneserNeyEstimator wglm = Estimators.WEIGHTEDSUM_GLM;
        CacheSpecification requiredCache = wglm.getRequiredCache(5).withWords().withProgress();
        //        HashMapCache cache = (HashMapCache) requiredCache.withCacheImplementation(
        //                CacheImplementation.HASH_MAP).build(
        //                        testCorpus.getGlmtk().getPaths());
        CompletionTrieCache cache = (CompletionTrieCache) requiredCache.withCacheImplementation(
                CacheImplementation.COMPLETION_TRIE).build(
                        testCorpus.getGlmtk().getPaths());

        glm.setCache(cache);
        wglm.setCache(cache);

        NGram sequence = new NGram("over");
        NGram history = new NGram("the", "United", "States");

        System.out.println(glm);
        System.out.println(history + " : " + sequence + " -> "
                + glm.probability(sequence, history));
        System.out.println(wglm);
        System.out.println(history + " : " + sequence + " -> "
                + wglm.probability(sequence, history));
        WeightedSumFunction func = wglm.calcWeightedSumFunction(history);
        System.out.println("size = " + func.size());
        System.out.println("patterns = "
                + StringUtils.join(func.getPatterns(), ", "));

        CompletionTrie trie = cache.getCountCompletionTrie(Patterns.get("1111"));
        Iterator<CompletionTrieEntry> iter = trie.getCompletions("the United States Ce");
        while (iter.hasNext()) {
            CompletionTrieEntry entry = iter.next();
            System.out.println(entry.getString() + "\t" + entry.getScore());
        }

        //        CompletionTrie trie = saCache.getCountCompletionTrie(Pattern.CNT_PATTERN);
        //        for (CompletionTrieEntry entry : trie)
        //            System.out.println(entry.getString());

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
