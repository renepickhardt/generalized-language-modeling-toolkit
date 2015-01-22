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
import static org.junit.Assume.assumeNotNull;

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
import de.glmtk.querying.estimator.fast.FastModifiedKneserNeyAbsEstimator;
import de.glmtk.querying.estimator.fast.FastModifiedKneserNeyEstimator;
import de.glmtk.querying.estimator.learned.LearnedModKneserNeyEstimator;
import de.glmtk.testutil.TestCorporaTest;
import de.glmtk.testutil.TestCorpus;
import de.glmtk.util.HashUtils;

/**
 * Test optimized estimator implementation using the slower ones.
 */
@RunWith(Parameterized.class)
public class OptimizedEstimatorTest extends TestCorporaTest {
    private static final Logger LOGGER = Logger.get(OptimizedEstimatorTest.class);

    @Parameters(name = "{0}")
    public static Iterable<Object[]> data() {
        Estimator slowMknAbs = Estimators.MOD_KNESER_NEY_ABS;
        Estimator fastMknAbs = new FastModifiedKneserNeyAbsEstimator();
        fastMknAbs.setName("Fast-Modified-Kneser-Ney (Abs-Lower-Order)");
        Estimator learnedMknAbs = null;

        Estimator slowMkn = Estimators.MOD_KNESER_NEY;
        Estimator fastMkn = new FastModifiedKneserNeyEstimator();
        fastMkn.setName("Fast-Modified-Kneser-Ney");
        Estimator learnedMkn = new LearnedModKneserNeyEstimator();
        learnedMkn.setName("Learned-Modified-Kneser-Ney");

        Estimator slowGlmAbs = Estimators.GLM_ABS;
        Estimator fastGlmAbs = null;
        Estimator learnedGlmAbs = null;

        Estimator slowGlm = Estimators.GLM;
        Estimator fastGlm = null;
        Estimator learnedGlm = null;

        return Arrays.asList(new Object[][] {
                //@formatter:off
                {"ModKneserNey",        slowMkn,    fastMkn,    learnedMkn},
                {"ModKneserNey (Abs)",  slowMknAbs, fastMknAbs, learnedMknAbs},
                {"GeneralizedLM",       slowGlm,    fastGlm,    learnedGlm},
                {"GeneralizedLM (Abs)", slowGlmAbs, fastGlmAbs, learnedGlmAbs}
                //@formatter:on
        });
    }

    private static TestCorpus testCorpus = TestCorpus.EN0008T;
    private static Path testFile = Constants.TEST_RESSOURCES_DIR.resolve("en0008t.testing.5");

    /*
     * We have to use static data for what would otherwise be put into member
     * vaiables, because Junit ParameterizedRunner will instantiate the class
     * for every test method newly.
     */
    private static Set<String> initliazed = new HashSet<>();
    private static Map<String, String> hashSlow = new HashMap<>();
    private static Map<String, String> hashFast = new HashMap<>();
    private static Map<String, String> hashLearned = new HashMap<>();

    private String name;
    private Estimator fastEstimator;
    private Estimator learnedEstimator;

    /**
     * @param fastEstimator
     *            May be {@code null}.
     * @param learnedEstimator
     *            May be {@code null}.
     */
    public OptimizedEstimatorTest(String name,
                                  Estimator slowEstimator,
                                  Estimator fastEstimator,
                                  Estimator learnedEstimator) throws Exception {
        this.name = name;
        this.fastEstimator = fastEstimator;
        this.learnedEstimator = learnedEstimator;

        if (initliazed.contains(name))
            return;

        Set<Pattern> patterns = slowEstimator.getUsedPatterns(5);
        if (fastEstimator != null)
            patterns.addAll(fastEstimator.getUsedPatterns(5));
        if (learnedEstimator != null)
            patterns.addAll(learnedEstimator.getUsedPatterns(5));

        Cache cache = testCorpus.getCache(patterns);

        hashSlow.put(name, runEstimatorOnFile(slowEstimator, cache));
        if (fastEstimator != null)
            hashFast.put(name, runEstimatorOnFile(fastEstimator, cache));
        if (learnedEstimator != null)
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
        assumeNotNull(fastEstimator);
        assertEquals(hashSlow.get(name), hashFast.get(name));
    }

    @Test
    public void testLearned() {
        assumeNotNull(learnedEstimator);
        assertEquals(hashSlow.get(name), hashLearned.get(name));
    }
}
