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

import static de.glmtk.querying.estimator.Estimators.FAST_GLM;
import static de.glmtk.querying.estimator.Estimators.FAST_GLM_ABS;
import static de.glmtk.querying.estimator.Estimators.FAST_GLM_DEL;
import static de.glmtk.querying.estimator.Estimators.FAST_GLM_DEL_FRONT;
import static de.glmtk.querying.estimator.Estimators.FAST_GLM_SKP_AND_DEL;
import static de.glmtk.querying.estimator.Estimators.FAST_MKN;
import static de.glmtk.querying.estimator.Estimators.FAST_MKN_ABS;
import static de.glmtk.querying.estimator.Estimators.FAST_MKN_SKP;
import static de.glmtk.querying.estimator.Estimators.FAST_MLE;
import static de.glmtk.querying.estimator.Estimators.GLM;
import static de.glmtk.querying.estimator.Estimators.GLM_ABS;
import static de.glmtk.querying.estimator.Estimators.GLM_DEL;
import static de.glmtk.querying.estimator.Estimators.GLM_DEL_FRONT;
import static de.glmtk.querying.estimator.Estimators.GLM_SKP_AND_DEL;
import static de.glmtk.querying.estimator.Estimators.MKN;
import static de.glmtk.querying.estimator.Estimators.MKN_ABS;
import static de.glmtk.querying.estimator.Estimators.MKN_SKP;
import static de.glmtk.querying.estimator.Estimators.MLE;
import static de.glmtk.querying.estimator.Estimators.WEIGHTEDSUM_GLM;
import static de.glmtk.querying.estimator.Estimators.WEIGHTEDSUM_MKN;
import static de.glmtk.querying.estimator.Estimators.WEIGHTEDSUM_MKN_SKP;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import de.glmtk.Constants;
import de.glmtk.Glmtk;
import de.glmtk.GlmtkPaths;
import de.glmtk.cache.Cache;
import de.glmtk.cache.CacheSpecification;
import de.glmtk.common.Pattern;
import de.glmtk.logging.Logger;
import de.glmtk.output.ProgressBar;
import de.glmtk.querying.estimator.Estimator;
import de.glmtk.querying.probability.QueryExecutor;
import de.glmtk.querying.probability.QueryMode;
import de.glmtk.testutil.TestCorporaTest;
import de.glmtk.testutil.TestCorpus;
import de.glmtk.util.NioUtils;

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
    private static final String PHASE_QUERYING = "Querying";

    private static TestCorpus testCorpus = TestCorpus.EN0008T;
    private static Path testFile = Constants.TEST_RESSOURCES_DIR.resolve("en0008t.testing.5");

    private static Cache cache = null;

    @Parameters(name = "{1}")
    public static Iterable<Object[]> data() {
        //@formatter:off
        return Arrays.asList(new Object[][] {
                {MLE, FAST_MLE},

                {MKN_ABS, FAST_MKN_ABS},

                {MKN, FAST_MKN},
                {MKN, WEIGHTEDSUM_MKN},

                {MKN_SKP, FAST_MKN_SKP},
                {MKN_SKP, WEIGHTEDSUM_MKN_SKP},

                {GLM_ABS, FAST_GLM_ABS},

                {GLM, FAST_GLM},
                {GLM, WEIGHTEDSUM_GLM},

                {GLM_DEL, FAST_GLM_DEL},

                {GLM_DEL_FRONT, FAST_GLM_DEL_FRONT},

                {GLM_SKP_AND_DEL, FAST_GLM_SKP_AND_DEL}
        });
        //@formatter:on
    }

    @BeforeClass
    public static void setUpCache() throws Exception {
        if (cache != null)
            return;

        CacheSpecification requiredCache = new CacheSpecification();
        for (Object[] params : data()) {
            Estimator exptected = (Estimator) params[0];
            Estimator actual = (Estimator) params[1];
            requiredCache.addAll(exptected.getRequiredCache(5));
            requiredCache.addAll(actual.getRequiredCache(5));
        }

        Set<Pattern> requiredPatterns = requiredCache.getRequiredPatterns();

        Glmtk glmtk = testCorpus.getGlmtk();
        glmtk.count(requiredPatterns);

        GlmtkPaths queryCache = glmtk.provideQueryCache(testFile,
                requiredPatterns);

        cache = requiredCache.withProgress().build(queryCache);
    }

    private Estimator expected;
    private Estimator actual;

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

        expected.setCache(cache);
        actual.setCache(cache);

        QueryMode queryMode = QueryMode.newSequence();
        QueryExecutor executorExpected = new QueryExecutor(paths, queryMode,
                expected, 5);
        QueryExecutor executorActual = new QueryExecutor(paths, queryMode,
                actual, 5);

        ProgressBar progressBar = new ProgressBar(PHASE_QUERYING,
                NioUtils.countNumberOfLines(testFile));

        Logger.setTraceEnabled(false);
        try (BufferedReader reader = Files.newBufferedReader(testFile,
                Constants.CHARSET)) {
            String line;
            while ((line = reader.readLine()) != null) {
                double probExpected = Double.NaN, probActual = Double.NaN;
                try {
                    probExpected = executorExpected.querySequence(line);
                    probActual = executorActual.querySequence(line);
                    if (Double.isNaN(probActual)
                            || Math.abs(probExpected - probActual) > Math.abs(probExpected) / 1e6)
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

                progressBar.increase();
            }
        }
        Logger.setTraceEnabled(true);

        LOGGER.info("passed");
    }
}
