/*
 * Generalized Language Modeling Toolkit (GLMTK)
 *
 * Copyright (C) 2014-2015 Lukas Schmelzeisen
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

package de.glmtk.testutil;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import de.glmtk.Constants;
import de.glmtk.Glmtk;
import de.glmtk.cache.Cache;
import de.glmtk.cache.CacheBuilder;
import de.glmtk.common.Config;
import de.glmtk.common.Pattern;
import de.glmtk.common.Patterns;

/**
 * Make sure you are inherting {@link TestCorporaTest} when writing a test using
 * that wants to use this class.
 */
public enum TestCorpus {
    ABC,
    MOBYDICK,
    EN0008T;

    public static void intializeTestCorpora(Config config) throws Exception {
        for (TestCorpus testCorpus : values())
            testCorpus.intialize(config);
    }

    private String corpusName;
    private Path corpus;
    private Glmtk glmtk;
    private String[] tokens;

    /**
     * Needed initialization method, because we can't add parameters to a enums
     * constructor.
     */
    private void intialize(Config config) throws Exception {
        corpusName = toString();
        corpus = Constants.TEST_RESSOURCES_DIR.resolve(corpusName.toLowerCase());

        Path workingDir = Constants.TEST_RESSOURCES_DIR.resolve(corpusName.toLowerCase()
                + Constants.WORKING_DIR_SUFFIX);
        glmtk = new Glmtk(config, corpus, workingDir);
        tokens = null;
    }

    public String getCorpusName() {
        return corpusName;
    }

    public Path getCorpus() {
        return corpus;
    }

    public Glmtk getGlmtk() {
        return glmtk;
    }

    /**
     * Lazily loaded, then cached.
     */
    public String[] getTokens() throws Exception {
        if (tokens == null) {
            Set<Pattern> neededPatterns = Patterns.getMany("1");
            if (!glmtk.getStatus().getCounted().containsAll(neededPatterns))
                glmtk.count(neededPatterns);
            Cache cache = new CacheBuilder().withCounts(neededPatterns).build(
                    glmtk.getPaths());
            Set<String> tokens = cache.getWords();
            this.tokens = tokens.toArray(new String[tokens.size()]);
        }
        return tokens;
    }

    public List<String> getSequenceList(int n,
                                        int length) throws Exception {
        List<String> result = new LinkedList<>();
        for (int k = 0; k != length; ++k) {
            result.add(getTokens()[n % getTokens().length]);
            n /= getTokens().length;
        }
        Collections.reverse(result);
        return result;
    }
}
