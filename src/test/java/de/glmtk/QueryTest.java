package de.glmtk;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import org.junit.Test;

import de.glmtk.cache.Cache;
import de.glmtk.cache.CacheSpecification;
import de.glmtk.common.NGram;
import de.glmtk.common.Pattern;
import de.glmtk.common.Patterns;
import de.glmtk.logging.Log4jHelper;
import de.glmtk.logging.Logger.Level;
import de.glmtk.querying.estimator.Estimator;
import de.glmtk.querying.estimator.Estimators;
import de.glmtk.testutil.TestCorporaTest;


/**
 * Playground to experiment with quick and dirty code ideas.
 */
public class QueryTest extends TestCorporaTest {
    @Test
    public void test() throws Exception {
        Log4jHelper.setLogLevel(Level.TRACE);
        // TestCorpus testCorpus = TestCorpus.EN0008T;
        // InterpolEstimator glm = Estimators.GLM;
        // WeightedSumModKneserNeyEstimator wglm = Estimators.WEIGHTEDSUM_GLM;
        // CacheSpecification requiredCache =
        // wglm.getRequiredCache(5).withWords().withProgress();
        // HashMapCache cache = (HashMapCache)
        // requiredCache.withCacheImplementation(
        // CacheImplementation.HASH_MAP).build(
        // testCorpus.getGlmtk().getPaths());
        // CompletionTrieCache cache = (CompletionTrieCache)
        // requiredCache.withCacheImplementation(
        // CacheImplementation.COMPLETION_TRIE).build(
        // testCorpus.getGlmtk().getPaths());
        //
        // glm.setCache(cache);
        // wglm.setCache(cache);
        //
        // NGram sequence = new NGram("over");
        // NGram history = new NGram("the", "United", "States");
        //
        // System.out.println(glm);
        // System.out.println(history + " : " + sequence + " -> "
        // + glm.probability(sequence, history));
        // System.out.println(wglm);
        // System.out.println(history + " : " + sequence + " -> "
        // + wglm.probability(sequence, history));
        // WeightedSumFunction func = wglm.calcWeightedSumFunction(history);
        // System.out.println("size = " + func.size());
        // System.out.println("patterns = "
        // + StringUtils.join(func.getPatterns(), ", "));
        //
        // CompletionTrie trie =
        // cache.getCountCompletionTrie(Patterns.get("1111"));
        // Iterator<CompletionTrieEntry> iter = trie.getCompletions("the United
        // States Ce");
        // while (iter.hasNext()) {
        // CompletionTrieEntry entry = iter.next();
        // System.out.println(entry.getString() + "\t" + entry.getScore());
        // }

        // CompletionTrie trie =
        // saCache.getCountCompletionTrie(Pattern.CNT_PATTERN);
        // for (CompletionTrieEntry entry : trie)
        // System.out.println(entry.getString());

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
        // for (String string : list) {
        // List<String> split = StringUtils.split(string, ' ');
        // NGram sequence = new NGram(split.remove(split.size() - 1));
        // NGram history = new NGram(split);
        //
        // // System.out.println("### " + history + " | " + sequence);
        //
        // estimator.probability(sequence, history);
        //
        // // WeightedSumFunction weightedSumFunction =
        // estimator.calcWeightedSumFunction(history);
        // // System.out.println(weightedSumFunction);
        //
        // // LOGGER.info(" %e ", estimator.probability(sequence,
        // // weightedSumFunction));
        // }

        Path corpus = Paths.get(
            "/home/lukas/langmodels/workspace/generalized-language-modeling-toolkit/src/test/resources/abc");
        Path workingDir = Paths.get(
            "/home/lukas/langmodels/workspace/generalized-language-modeling-toolkit/src/test/resources/abc.glmtk");
        // Path corpus = Paths.get(
        // "/home/lukas/langmodels/data/oanc.expsetup/training-5");
        // Path workingDir = Paths.get(
        // "/home/lukas/langmodels/data/oanc.expsetup/training-5.glmtk");
        Glmtk glmtk = new Glmtk(config, corpus, workingDir);

        Path queryFile =
            Paths.get("/home/lukas/langmodels/data/oanc.expsetup/ngram-5");

        int neededOrder = 5;

        Estimator estimator =
            Estimators.DIFF_INTERPOL_ABS_DISCOUNT_MLE_CMLE_SKP;
        // Estimator estimator = Estimators.DIFF_INTERPOL_ABS_DISCOUNT_MLE_SKP;

        CacheSpecification cacheSpec = estimator.getRequiredCache(neededOrder);
        cacheSpec.withCounts(Patterns.getMany("x")).withWords().withProgress();

        Set<Pattern> requiredPatterns = cacheSpec.getRequiredPatterns();
        requiredPatterns.add(Patterns.get("x1111x"));

        GlmtkPaths paths = glmtk.getPaths();
        GlmtkPaths cachePaths = paths;
        // GlmtkPaths cachePaths = glmtk.provideQueryCache(queryFile,
        // requiredPatterns);
        // GlmtkPaths cachePaths = glmtk.provideArgmaxQueryCache(queryFile,
        // requiredPatterns);

        // CompletionTrieCache sortedAccessCache = (CompletionTrieCache)
        // cacheSpec.withCacheImplementation(
        // CacheImplementation.COMPLETION_TRIE).build(cachePaths);
        Cache sortedAccessCache = cacheSpec.build(cachePaths);

        // NGram history = new NGram("comment", "of", "Robert", "J");
        // NGram sequence = new NGram(".");
        NGram history = new NGram("a", "b", "a");
        NGram sequence = new NGram("b");

        estimator.setCache(sortedAccessCache);
        double prob = estimator.probability(sequence, history);
        System.out.println("P(" + sequence + " | " + history + ") = " + prob);

        // String s1 = "for";
        // String s2 = "mid";
        // int k = 5;
        //
        // estimator.setCache(sortedAccessCache);
        // ThresholdArgmaxQueryExecutor thresholdArgmaxQueryExecutor = new
        // ThresholdArgmaxQueryExecutor(
        // estimator, sortedAccessCache, sortedAccessCache);
        // System.out.println("\nTA");
        // long t1 = System.nanoTime();
        // System.out.println(thresholdArgmaxQueryExecutor.queryArgmax(s1, k));
        // long t2 = System.nanoTime();
        // System.out.println((t2 - t1) + "ns");
        // System.out.println(thresholdArgmaxQueryExecutor.getNumSortedAccesses()
        // + " " + thresholdArgmaxQueryExecutor.getNumRandomAccesses());
        // long t3 = System.nanoTime();
        // System.out.println(thresholdArgmaxQueryExecutor.queryArgmax(s2, k));
        // long t4 = System.nanoTime();
        // System.out.println((t4 - t3) + "ns");
        // System.out.println(thresholdArgmaxQueryExecutor.getNumSortedAccesses()
        // + " " + thresholdArgmaxQueryExecutor.getNumSortedAccesses());
        //
        // NoRandomAccessArgmaxQueryExecutor noRandomAccessArgmaxQueryExecutor =
        // new NoRandomAccessArgmaxQueryExecutor(
        // estimator, sortedAccessCache);
        // noRandomAccessArgmaxQueryExecutor.setProbbabilityDislay(
        // ProbabilityDislay.AVERAGE);
        // System.out.println("\nNRA");
        // long t5 = System.nanoTime();
        // System.out.println(noRandomAccessArgmaxQueryExecutor.queryArgmax(s1,
        // k));
        // long t6 = System.nanoTime();
        // System.out.println((t6 - t5) + "ns");
        // System.out.println(
        // noRandomAccessArgmaxQueryExecutor.getNumSortedAccesses());
        // long t7 = System.nanoTime();
        // System.out.println(noRandomAccessArgmaxQueryExecutor.queryArgmax(s2,
        // k));
        // long t8 = System.nanoTime();
        // System.out.println((t8 - t7) + "ns");
        // System.out.println(
        // noRandomAccessArgmaxQueryExecutor.getNumSortedAccesses());
        //
        // TrivialArgmaxQueryExecutor trivialArgmaxQueryExecutor = new
        // TrivialArgmaxQueryExecutor(
        // estimator, sortedAccessCache);
        // System.out.println("\nSMPL");
        // long t9 = System.nanoTime();
        // System.out.println(trivialArgmaxQueryExecutor.queryArgmax(s1, k));
        // long t10 = System.nanoTime();
        // System.out.println((t10 - t9) + "ns");
        // long t11 = System.nanoTime();
        // System.out.println(trivialArgmaxQueryExecutor.queryArgmax(s2, k));
        // long t12 = System.nanoTime();
        // System.out.println((t12 - t11) + "ns");
    }
}
