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

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

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

/**
 * Test optimized estimator implementations using the slower ones. Test whether
 * two estimators return identical probabilities for all sequences from a test
 * file.
 *
 * <p>
 * Typically we use this test to verify Estimators we can not test with
 * {@link EstimatorTest} because their definitions require larger corpora, for
 * which we can not test of all possibles sequences.
 */
@RunWith(Parameterized.class)
public class EstimatorEqualsTest extends TestCorporaTest {
    private static final Logger LOGGER = Logger.get(EstimatorEqualsTest.class);

    @Parameters(name = "{1}")
    public static Iterable<Object[]> data() {
        Estimator fastMknAbs = new FastModKneserNeyAbsEstimator();
        fastMknAbs.setName("Fast-Modified-Kneser-Ney (Abs-Lower-Order)");
        Estimator fastMkn = new FastModKneserNeyEstimator();
        fastMkn.setName("Fast-Modified-Kneser-Ney");
        Estimator learnedMkn = new LearnedModKneserNeyEstimator();
        learnedMkn.setName("Learned-Modified-Kneser-Ney");

        return Arrays.asList(new Object[][] {
                {Estimators.MOD_KNESER_NEY_ABS, fastMknAbs},
                {Estimators.MOD_KNESER_NEY, fastMkn},
                {Estimators.MOD_KNESER_NEY, learnedMkn}});
    }

    private static TestCorpus testCorpus = TestCorpus.EN0008T;
    private static Path testFile = Constants.TEST_RESSOURCES_DIR.resolve("en0008t.testing.5");

    public Estimator expected;
    public Estimator actual;

    public EstimatorEqualsTest(Estimator expected,
                               Estimator actual) {
        this.expected = expected;
        this.actual = actual;
    }

    @Test
    public void testEstimatorEquals() throws Throwable {
        LOGGER.info("=== Testing if %s equals %s", actual, expected);

        Glmtk glmtk = testCorpus.getGlmtk();
        GlmtkPaths paths = glmtk.getPaths();

        Cache cacheExpected = expected.getRequiredCache(5).build(paths);
        Cache cacheActual = actual.getRequiredCache(5).build(paths);
        expected.setCache(cacheExpected);
        actual.setCache(cacheActual);

        QueryMode queryMode = QueryMode.newCond(5);
        QueryExecutor executorExpected = new QueryExecutor(paths, queryMode,
                expected, 5);
        QueryExecutor executorActual = new QueryExecutor(paths, queryMode,
                actual, 5);

        Logger.setTraceEnabled(false);
        try (BufferedReader reader = Files.newBufferedReader(testFile,
                Constants.CHARSET)) {
            String line;
            while ((line = reader.readLine()) != null) {
                double probExpected = Double.NaN, probActual = Double.NaN;
                try {
                    probExpected = executorExpected.querySequence(line);
                    probActual = executorActual.querySequence(line);
                    if (Math.abs(probExpected - probActual) > Math.abs(probExpected) / 1e6)
                        throw new Exception("failAssert");
                } catch (Throwable t) {
                    Logger.setTraceEnabled(true);
                    executorExpected.querySequence(line);
                    executorActual.querySequence(line);
                    Logger.setTraceEnabled(false);

                    if (t.getMessage().equals("failAssert"))
                        fail(String.format("Expected <%e> but was <%e>.",
                                probExpected, probActual));

                    throw t;
                }
            }
        }
        Logger.setTraceEnabled(true);

        LOGGER.info("passed");
    }
}
