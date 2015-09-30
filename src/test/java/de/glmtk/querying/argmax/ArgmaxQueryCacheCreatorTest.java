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

import java.nio.file.Path;
import java.util.Set;

import org.junit.Test;

import de.glmtk.Constants;
import de.glmtk.Glmtk;
import de.glmtk.GlmtkPaths;
import de.glmtk.cache.CacheSpecification;
import de.glmtk.common.Pattern;
import de.glmtk.common.Status;
import de.glmtk.querying.estimator.Estimators;
import de.glmtk.querying.probability.QueryCacherCreator;
import de.glmtk.testutil.TestCorporaTest;
import de.glmtk.testutil.TestCorpus;


public class ArgmaxQueryCacheCreatorTest extends TestCorporaTest {
    private static final TestCorpus testCorpus = TestCorpus.EN0008T;

    @Test
    public void test() throws Exception {
        CacheSpecification cacheSpec = Estimators.GLM.getRequiredCache(4);
        Set<Pattern> requiredPatterns = cacheSpec.getRequiredPatterns();

        Glmtk glmtk = testCorpus.getGlmtk();
        glmtk.count(requiredPatterns);

        Path queryFile =
            Constants.TEST_RESSOURCES_DIR.resolve("en0008t.argmax.query");
        Path vocabFile =
            Constants.TEST_RESSOURCES_DIR.resolve("en0008t.argmax.vocab");
        Path crossFile =
            Constants.TEST_RESSOURCES_DIR.resolve("en0008t.argmax.cross");

        Status status = glmtk.getStatus();
        GlmtkPaths paths = glmtk.getPaths();

        ArgmaxQueryCacheCreator argmaxQueryCacheCreator =
            new ArgmaxQueryCacheCreator(config);
        argmaxQueryCacheCreator.createQueryCache("argmaxquerycache", queryFile,
            false, vocabFile, requiredPatterns, status, paths);

        QueryCacherCreator queryCacheCreator = new QueryCacherCreator(config);
        queryCacheCreator.createQueryCache("crossquerycache", crossFile, false,
            requiredPatterns, status, paths);

        // TODO: recursively diff both created directories
    }
}
