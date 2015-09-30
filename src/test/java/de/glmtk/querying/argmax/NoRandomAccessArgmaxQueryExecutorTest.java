/*
 * Generalized Language Modeling Toolkit (GLMTK)
 *
 * Copyright (C) 2015 Lukas Schmelzeisen
 *
 * GLMTK is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * GLMTK is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * GLMTK. If not, see <http://www.gnu.org/licenses/>.
 *
 * See the AUTHORS file for contributors.
 */

package de.glmtk.querying.argmax;

import static de.glmtk.Constants.TEST_RESSOURCES_DIR;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

import de.glmtk.Constants;
import de.glmtk.cache.CacheSpecification;
import de.glmtk.cache.CacheSpecification.CacheImplementation;
import de.glmtk.cache.CompletionTrieCache;
import de.glmtk.querying.argmax.ArgmaxQueryExecutor.ArgmaxResult;
import de.glmtk.querying.argmax.NoRandomAccessArgmaxQueryExecutor.ProbabilityDislay;
import de.glmtk.querying.estimator.Estimators;
import de.glmtk.querying.estimator.weightedsum.WeightedSumGenLangModelEstimator;
import de.glmtk.querying.estimator.weightedsum.WeightedSumModKneserNeyEstimator;
import de.glmtk.testutil.TestCorporaTest;
import de.glmtk.testutil.TestCorpus;


public class NoRandomAccessArgmaxQueryExecutorTest extends TestCorporaTest {
    private static final TestCorpus TEST_CORPUS = TestCorpus.EN0008T;
    private static final Path VOCAB_FILE =
        TEST_RESSOURCES_DIR.resolve("en0008t.argmax.vocab");
    private static final Path QUERY_FILE =
        TEST_RESSOURCES_DIR.resolve("en0008t.argmax.query");

    private static Set<String> vocab;
    private static List<String> queries;

    private static CompletionTrieCache cache;

    @BeforeClass
    public static void loadFiles() throws IOException {
        vocab = new HashSet<>();
        try (BufferedReader reader =
            Files.newBufferedReader(VOCAB_FILE, Constants.CHARSET)) {
            String line;
            while ((line = reader.readLine()) != null) {
                vocab.add(line);
            }
        }

        queries = new ArrayList<>();
        try (BufferedReader reader =
            Files.newBufferedReader(QUERY_FILE, Constants.CHARSET)) {
            String line;
            while ((line = reader.readLine()) != null) {
                queries.add(line);
            }
        }
    }

    @BeforeClass
    public static void loadCache() throws IOException {
        CacheSpecification requiredCache = new CacheSpecification()
            .withCacheImplementation(CacheImplementation.COMPLETION_TRIE);
        requiredCache.addAll(Estimators.WEIGHTEDSUM_MKN.getRequiredCache(5));
        requiredCache.addAll(Estimators.WEIGHTEDSUM_GLM.getRequiredCache(5));
        cache = (CompletionTrieCache) requiredCache.withProgress()
            .build(TEST_CORPUS.getGlmtk().getPaths());
    }

    @Test
    public void testMkn() {
        WeightedSumModKneserNeyEstimator estimator = Estimators.WEIGHTEDSUM_MKN;

        System.out.format("=== %s%n", estimator);

        estimator.setCache(cache);
        NoRandomAccessArgmaxQueryExecutor argmaxQueryExecutor =
            new NoRandomAccessArgmaxQueryExecutor(estimator, cache);
        argmaxQueryExecutor.setProbbabilityDislay(ProbabilityDislay.EXACT);

        for (String query : queries) {
            System.out.format("# %s:%n", query);
            long t1 = System.currentTimeMillis();
            List<ArgmaxResult> results =
                argmaxQueryExecutor.queryArgmax(query, 5);
            long t2 = System.currentTimeMillis();
            printArgmaxResults(results);
            System.out.format("took %dms%n%n", t2 - t1);
        }
    }

    @Test
    public void testGlm() {
        WeightedSumGenLangModelEstimator estimator = Estimators.WEIGHTEDSUM_GLM;

        System.out.format("=== %s%n", estimator);

        estimator.setCache(cache);
        NoRandomAccessArgmaxQueryExecutor argmaxQueryExecutor =
            new NoRandomAccessArgmaxQueryExecutor(estimator, cache);
        argmaxQueryExecutor.setProbbabilityDislay(ProbabilityDislay.EXACT);

        for (String query : queries) {
            System.out.format("# %s:%n", query);
            long t1 = System.currentTimeMillis();
            List<ArgmaxResult> results =
                argmaxQueryExecutor.queryArgmax(query, 5);
            long t2 = System.currentTimeMillis();
            printArgmaxResults(results);
            System.out.format("took %dms%n%n", t2 - t1);
        }
    }

    private void printArgmaxResults(List<ArgmaxResult> results) {
        for (ArgmaxResult result : results) {
            System.out.println(result);
        }
    }
}
