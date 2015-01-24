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
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import de.glmtk.Constants;
import de.glmtk.Glmtk;
import de.glmtk.GlmtkPaths;
import de.glmtk.cache.Cache;
import de.glmtk.logging.Logger;
import de.glmtk.querying.estimator.Estimator;
import de.glmtk.querying.estimator.Estimators;
import de.glmtk.querying.estimator.fast.FastModKneserNeyAbsEstimator;
import de.glmtk.querying.estimator.fast.FastModKneserNeyEstimator;
import de.glmtk.querying.estimator.learned.LearnedModKneserNeyEstimator;
import de.glmtk.testutil.TestCorporaTest;
import de.glmtk.testutil.TestCorpus;
import de.glmtk.util.HashUtils;

/**
 * Test optimized estimator implementations using the slower ones.
 *
 * Typically we can not test these with {@link EstimatorTest} because
 * Modified-Kneser-Ney discounting is not defined on small corpora.
 */
@RunWith(Parameterized.class)
public class OptimizedEstimatorTest extends TestCorporaTest {
    private static final Logger LOGGER = Logger.get(OptimizedEstimatorTest.class);

    @Parameters(name = "{0}")
    public static Iterable<Object[]> data() {
        Estimator fastMknAbs = new FastModKneserNeyAbsEstimator();
        fastMknAbs.setName("Fast-Modified-Kneser-Ney (Abs-Lower-Order)");
        Estimator fastMkn = new FastModKneserNeyEstimator();
        fastMkn.setName("Fast-Modified-Kneser-Ney");
        Estimator learnedMkn = new LearnedModKneserNeyEstimator();
        learnedMkn.setName("Learned-Modified-Kneser-Ney");

        return Arrays.asList(new Object[][] {
                {fastMknAbs, Estimators.MOD_KNESER_NEY_ABS},
                {fastMkn, Estimators.MOD_KNESER_NEY},
                {learnedMkn, Estimators.MOD_KNESER_NEY}});
    }

    private static TestCorpus testCorpus = TestCorpus.EN0008T;
    private static Path testFile = Constants.TEST_RESSOURCES_DIR.resolve("en0008t.testing.5");

    private static Map<Estimator, String> hashes = new HashMap<>();

    public Estimator optimized;
    public Estimator slow;

    public OptimizedEstimatorTest(Estimator optimized,
                                  Estimator slow) throws Exception {
        this.optimized = optimized;
        this.slow = slow;

        if (!hashes.containsKey(optimized))
            hashes.put(optimized, getHashForEstimatedTestFile(optimized));
        if (!hashes.containsKey(slow))
            hashes.put(slow, getHashForEstimatedTestFile(slow));
    }

    private String getHashForEstimatedTestFile(Estimator estimator) throws Exception {
        Glmtk glmtk = testCorpus.getGlmtk();
        GlmtkPaths paths = glmtk.getPaths();

        Cache cache = estimator.getRequiredCache(5).build(paths);
        estimator.setCache(cache);

        Path outputFile = paths.getQueriesDir().resolve(
                testFile.getFileName() + " " + estimator.toString());
        Files.createDirectories(outputFile.getParent());

        long timeBefore = System.currentTimeMillis();
        glmtk.queryFile(QueryMode.newSequence(), estimator, 5, testFile,
                outputFile);
        long timeAfter = System.currentTimeMillis();
        LOGGER.info("Estimator %s took %dms.", estimator, timeAfter
                - timeBefore);

        return HashUtils.generateMd5Hash(outputFile);
    }

    @Test
    public void testHashes() {
        assertEquals(hashes.get(optimized), hashes.get(slow));
    }
}
