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

package de.glmtk.querying;

import static org.junit.Assert.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import de.glmtk.Constants;
import de.glmtk.Glmtk;
import de.glmtk.common.Cache;
import de.glmtk.common.Pattern;
import de.glmtk.logging.Logger;
import de.glmtk.querying.estimator.Estimator;
import de.glmtk.querying.estimator.Estimators;
import de.glmtk.querying.estimator.fast.FastModifiedKneserNeyEstimator;
import de.glmtk.querying.estimator.learned.LearnedModKneserNeyEstimator;
import de.glmtk.testutil.TestCorporaTest;
import de.glmtk.testutil.TestCorpus;
import de.glmtk.util.HashUtils;

@RunWith(Parameterized.class)
public class FastEstimatorTest extends TestCorporaTest {
    private static final Logger LOGGER = Logger.get(FastEstimatorTest.class);

    @Parameters(name = "{0}")
    public static Iterable<Object[]> data() {
        Estimator slowMkn = Estimators.MOD_KNESER_NEY;
        Estimator fastMkn = new FastModifiedKneserNeyEstimator();
        fastMkn.setName("Fast-Modified-Kneser-Ney");
        Estimator learnedMkn = new LearnedModKneserNeyEstimator();
        learnedMkn.setName("Learned-Modified-Kneser-Ney");

        return Arrays.asList(new Object[][] {{"ModKneserNey", slowMkn, fastMkn,
                learnedMkn}});
    }

    private static TestCorpus testCorpus = TestCorpus.EN0008T;
    private static Path testFile = Constants.TEST_RESSOURCES_DIR.resolve("en0008t.testing.5");

    private static Set<String> initliazed = new HashSet<>();
    private static Map<String, String> hashSlow = new HashMap<>();
    private static Map<String, String> hashFast = new HashMap<>();
    private static Map<String, String> hashLearned = new HashMap<>();

    private String name;

    public FastEstimatorTest(String name,
                             Estimator slowEstimator,
                             Estimator fastEstimator,
                             Estimator learnedEstimator) throws Exception {
        this.name = name;

        if (initliazed.contains(name))
            return;

        Set<Pattern> patternsSlow = slowEstimator.getUsedPatterns(5);
        Set<Pattern> patternsFast = fastEstimator.getUsedPatterns(5);
        Set<Pattern> patternsLearned = learnedEstimator.getUsedPatterns(5);

        Set<Pattern> patterns = new HashSet<>();
        patterns.addAll(patternsSlow);
        patterns.addAll(patternsFast);
        patterns.addAll(patternsLearned);

        LOGGER.debug("patternsSlow    : %s", patternsSlow);
        LOGGER.debug("patternsFast    : %s", patternsFast);
        LOGGER.debug("patternsLearned : %s", patternsLearned);
        LOGGER.debug("patterns        : %s", patterns);

        Cache cache = testCorpus.getCache(patterns);

        hashSlow.put(name, runEstimatorOnFile(slowEstimator, cache));
        hashFast.put(name, runEstimatorOnFile(fastEstimator, cache));
        hashLearned.put(name, runEstimatorOnFile(learnedEstimator, cache));

        initliazed.add(name);
    }

    private String runEstimatorOnFile(Estimator estimator,
                                      Cache cache) throws Exception {
        Glmtk glmtk = testCorpus.getGlmtk();

        QueryMode queryMode = QueryMode.newSequence();
        int corpusOrder = 5;
        Path outputFile = glmtk.getPaths().getQueriesDir().resolve(
                testFile.getFileName() + " " + estimator.toString());

        estimator.setCache(cache);
        Files.createDirectories(outputFile.getParent());

        long t1 = System.currentTimeMillis();
        glmtk.queryFile(queryMode, estimator, corpusOrder, testFile, outputFile);
        long t2 = System.currentTimeMillis();
        LOGGER.info("Estimator %s took %dms.", estimator, t2 - t1);

        return HashUtils.generateMd5Hash(outputFile);
    }

    @Test
    public void testFast() {
        assertEquals(hashSlow.get(name), hashFast.get(name));
    }

    @Test
    public void testLearned() {
        assertEquals(hashSlow.get(name), hashLearned.get(name));
    }
}
