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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import de.glmtk.Constants;
import de.glmtk.Glmtk;
import de.glmtk.GlmtkPaths;
import de.glmtk.cache.Cache;
import de.glmtk.cache.CacheBuilder;
import de.glmtk.common.Pattern;
import de.glmtk.logging.Logger;
import de.glmtk.querying.estimator.Estimator;
import de.glmtk.querying.estimator.Estimators;
import de.glmtk.querying.probability.QueryMode;
import de.glmtk.testutil.TestCorporaTest;
import de.glmtk.testutil.TestCorpus;

@RunWith(Parameterized.class)
public class EstimatorSpeedTest extends TestCorporaTest {
    private static final Logger LOGGER = Logger.get(EstimatorSpeedTest.class);

    private static TestCorpus testCorpus = TestCorpus.EN0008T;
    private static Path testFile = Constants.TEST_RESSOURCES_DIR.resolve("en0008t.testing.5");

    private static Cache cache = null;

    private static Map<Estimator, Long> results = new LinkedHashMap<>();

    @Parameters(name = "{0}")
    public static Iterable<Object[]> data() {
        //@formatter:off
        return Arrays.asList(new Object[][]{
                {Estimators.MKN_ABS},
                {Estimators.FAST_MKN_ABS},

                {Estimators.MKN},
                {Estimators.FAST_MKN},
                {Estimators.WEIGHTEDSUM_MKN},

                {Estimators.GLM_ABS},
                {Estimators.FAST_GLM_ABS},

                {Estimators.GLM},
                {Estimators.FAST_GLM},
                {Estimators.WEIGHTEDSUM_GLM},

                {Estimators.WEIGHTEDSUM_AVERAGE}
        });
        //@formatter:on
    }

    @BeforeClass
    public static void setUpCache() throws Exception {
        if (cache != null)
            return;

        CacheBuilder requiredCache = new CacheBuilder().withProgress();
        for (Object[] params : data()) {
            Estimator estimator = (Estimator) params[0];
            requiredCache.addAll(estimator.getRequiredCache(5));
        }

        Set<Pattern> requiredPatterns = requiredCache.getRequiredPatterns();

        Glmtk glmtk = testCorpus.getGlmtk();
        glmtk.count(requiredPatterns);

        GlmtkPaths queryCache = glmtk.provideQueryCache(testFile,
                requiredPatterns);

        cache = requiredCache.withProgress().build(queryCache);
    }

    @AfterClass
    public static void displayResults() {
        int maxNameLength = 0;
        for (Estimator estimator : results.keySet())
            if (maxNameLength < estimator.toString().length())
                maxNameLength = estimator.toString().length();

        for (Entry<Estimator, Long> entry : results.entrySet()) {
            Estimator estimator = entry.getKey();
            Long speed = entry.getValue();

            LOGGER.info("%-" + maxNameLength + "s Estimator:  Querying: %6dms",
                    estimator, speed);
        }
    }

    private Estimator estimator;

    public EstimatorSpeedTest(Estimator estimator) {
        this.estimator = estimator;
    }

    @Test
    public void testSpeed() throws Exception {
        Runtime.getRuntime().gc();

        Glmtk glmtk = testCorpus.getGlmtk();
        GlmtkPaths paths = glmtk.getPaths();

        estimator.setCache(cache);

        Files.createDirectories(paths.getQueriesDir());
        Path outputFile = paths.getQueriesDir().resolve(
                testFile.getFileName() + " " + estimator.toString());

        long timeBeforeQuerying = System.currentTimeMillis();
        glmtk.queryFile(QueryMode.newSequence(), estimator, 5, testFile,
                outputFile);
        long timeAfterQuerying = System.currentTimeMillis();

        results.put(estimator, timeAfterQuerying - timeBeforeQuerying);
    }
}
