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

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

import de.glmtk.Constants;
import de.glmtk.Glmtk;
import de.glmtk.common.Pattern;
import de.glmtk.logging.Logger;
import de.glmtk.querying.estimator.Estimator;
import de.glmtk.querying.estimator.Estimators;
import de.glmtk.querying.estimator.fast.FastModifiedKneserNeyEstimator;
import de.glmtk.testutil.TestCorporaTest;
import de.glmtk.testutil.TestCorpus;

public class FastEstimatorTest extends TestCorporaTest {
    private static final Logger LOGGER = Logger.get(FastEstimatorTest.class);

    private static FileQueryExecutor fileQueryExecutor;

    @BeforeClass
    public static void setUpFileQueryExecutor() {
    }

    @Test
    public void testFastModifiedKneserEstimator() throws Exception {
        Estimator slowEstimator = Estimators.MOD_KNESER_NEY;
        FastModifiedKneserNeyEstimator fastEstimator = new FastModifiedKneserNeyEstimator();
        fastEstimator.setName("Fast-Modified-Kneser-Ney");

        Set<Pattern> patternsSlow = slowEstimator.getUsedPatterns(5);
        Set<Pattern> patternsFast = fastEstimator.getUsedPatterns(5);
        Set<Pattern> patterns = new HashSet<>();
        patterns.addAll(patternsSlow);
        patterns.addAll(patternsFast);

        LOGGER.debug("patternsSlow = %s", patternsSlow);
        LOGGER.debug("patternsFast = %s", patternsFast);
        LOGGER.debug("patterns     = %s", patterns);

        runEstimator(slowEstimator, patterns);
        runEstimator(fastEstimator, patterns);
    }

    private void runEstimator(Estimator estimator,
                              Set<Pattern> patterns) throws Exception {
        TestCorpus testCorpus = TestCorpus.EN0008T;

        QueryMode queryMode = QueryMode.newSequence();
        Path inputFile = Constants.TEST_RESSOURCES_DIR.resolve("en0008t.testing.5");
        estimator.setCache(testCorpus.getCache(patterns));
        int corpusOrder = 5;

        long t1 = System.currentTimeMillis();
        Glmtk glmtk = testCorpus.getGlmtk();
        glmtk.queryFile(queryMode, estimator, corpusOrder, inputFile);
        long t2 = System.currentTimeMillis();
        LOGGER.info("Estimator %s took %dms.", estimator, t2 - t1);
    }
}
