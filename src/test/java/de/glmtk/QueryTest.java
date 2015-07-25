package de.glmtk;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import org.junit.Test;

import de.glmtk.cache.CacheSpecification;
import de.glmtk.cache.CacheSpecification.CacheImplementation;
import de.glmtk.cache.CompletionTrieCache;
import de.glmtk.common.Pattern;
import de.glmtk.common.Patterns;
import de.glmtk.logging.Log4jHelper;
import de.glmtk.logging.Logger.Level;
import de.glmtk.querying.argmax.NoRandomAccessArgmaxQueryExecutor;
import de.glmtk.querying.argmax.NoRandomAccessArgmaxQueryExecutor.ProbabilityDislay;
import de.glmtk.querying.argmax.ThresholdArgmaxQueryExecutor;
import de.glmtk.querying.argmax.TrivialArgmaxQueryExecutor;
import de.glmtk.querying.estimator.Estimators;
import de.glmtk.querying.estimator.weightedsum.WeightedSumEstimator;
import de.glmtk.testutil.TestCorporaTest;

/**
 * Playground to experiment with quick and dirty code ideas.
 */
public class QueryTest extends TestCorporaTest {
    @Test
    public void test() throws Exception {
        Log4jHelper.setLogLevel(Level.TRACE);
        //        TestCorpus testCorpus = TestCorpus.EN0008T;
        //        InterpolEstimator glm = Estimators.GLM;
        //        WeightedSumModKneserNeyEstimator wglm = Estimators.WEIGHTEDSUM_GLM;
        //        CacheSpecification requiredCache = wglm.getRequiredCache(5).withWords().withProgress();
        //        HashMapCache cache = (HashMapCache) requiredCache.withCacheImplementation(
        //                CacheImplementation.HASH_MAP).build(
        //                        testCorpus.getGlmtk().getPaths());
        //        CompletionTrieCache cache = (CompletionTrieCache) requiredCache.withCacheImplementation(
        //                CacheImplementation.COMPLETION_TRIE).build(
        //                        testCorpus.getGlmtk().getPaths());
        //
        //        glm.setCache(cache);
        //        wglm.setCache(cache);
        //
        //        NGram sequence = new NGram("over");
        //        NGram history = new NGram("the", "United", "States");
        //
        //        System.out.println(glm);
        //        System.out.println(history + " : " + sequence + " -> "
        //                + glm.probability(sequence, history));
        //        System.out.println(wglm);
        //        System.out.println(history + " : " + sequence + " -> "
        //                + wglm.probability(sequence, history));
        //        WeightedSumFunction func = wglm.calcWeightedSumFunction(history);
        //        System.out.println("size = " + func.size());
        //        System.out.println("patterns = "
        //                + StringUtils.join(func.getPatterns(), ", "));
        //
        //        CompletionTrie trie = cache.getCountCompletionTrie(Patterns.get("1111"));
        //        Iterator<CompletionTrieEntry> iter = trie.getCompletions("the United States Ce");
        //        while (iter.hasNext()) {
        //            CompletionTrieEntry entry = iter.next();
        //            System.out.println(entry.getString() + "\t" + entry.getScore());
        //        }

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

        Path corpus = Paths.get("/home/lukas/langmodels/data/oanc.expsetup/training-5");
        Path workingDir = Paths.get("/home/lukas/langmodels/data/oanc.expsetup/training-5.glmtk");
        Glmtk glmtk = new Glmtk(config, corpus, workingDir);

        Path queryFile = Paths.get("/home/lukas/langmodels/data/oanc.expsetup/ngram-2-100");

        int neededOrder = 2;

        WeightedSumEstimator estimator = Estimators.WEIGHTEDSUM_MKN;

        CacheSpecification cacheSpec = estimator.getRequiredCache(neededOrder);
        cacheSpec.withCounts(Patterns.getMany("x")).withWords().withProgress();

        Set<Pattern> requiredPatterns = cacheSpec.getRequiredPatterns();
        requiredPatterns.add(Patterns.get("x1111x"));

        GlmtkPaths paths = glmtk.getPaths();
        GlmtkPaths cachePaths = paths;
        //        GlmtkPaths cachePaths = glmtk.provideArgmaxQueryCache(queryFile,
        //                requiredPatterns);

        CompletionTrieCache sortedAccessCache = (CompletionTrieCache) cacheSpec.withCacheImplementation(
                CacheImplementation.COMPLETION_TRIE).build(cachePaths);

        estimator.setCache(sortedAccessCache);
        ThresholdArgmaxQueryExecutor thresholdArgmaxQueryExecutor = new ThresholdArgmaxQueryExecutor(
                estimator, sortedAccessCache, sortedAccessCache);
        System.out.println("\nTA");
        System.out.println(thresholdArgmaxQueryExecutor.queryArgmax("in", 1));
        System.out.println(thresholdArgmaxQueryExecutor.getNumSortedAccesses()
                + " " + thresholdArgmaxQueryExecutor.getNumRandomAccesses());
        System.out.println(thresholdArgmaxQueryExecutor.queryArgmax(
                "improbably", 1));
        System.out.println(thresholdArgmaxQueryExecutor.getNumSortedAccesses()
                + " " + thresholdArgmaxQueryExecutor.getNumSortedAccesses());

        NoRandomAccessArgmaxQueryExecutor noRandomAccessArgmaxQueryExecutor = new NoRandomAccessArgmaxQueryExecutor(
                estimator, sortedAccessCache);
        noRandomAccessArgmaxQueryExecutor.setProbbabilityDislay(ProbabilityDislay.AVERAGE);
        System.out.println("\nNRA");
        System.out.println(noRandomAccessArgmaxQueryExecutor.queryArgmax("in",
                1));
        System.out.println(noRandomAccessArgmaxQueryExecutor.getNumSortedAccesses());
        System.out.println(noRandomAccessArgmaxQueryExecutor.queryArgmax(
                "improbably", 1));
        System.out.println(noRandomAccessArgmaxQueryExecutor.getNumSortedAccesses());

        TrivialArgmaxQueryExecutor trivialArgmaxQueryExecutor = new TrivialArgmaxQueryExecutor(
                estimator, sortedAccessCache);
        System.out.println("\nSMPL");
        System.out.println(trivialArgmaxQueryExecutor.queryArgmax("in", 1));
        System.out.println(trivialArgmaxQueryExecutor.queryArgmax("improbably",
                1));
    }
}
