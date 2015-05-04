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

package de.glmtk.cache;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import de.glmtk.Constants;
import de.glmtk.GlmtkPaths;
import de.glmtk.common.NGram;
import de.glmtk.common.Pattern;
import de.glmtk.files.CountsReader;
import de.glmtk.logging.Logger;
import de.glmtk.util.completiontrie.CompletionTrie;
import de.glmtk.util.completiontrie.CompletionTrieBuilder;

public class CompletionTrieCache extends AbstractCache {
    private static final Logger LOGGER = Logger.get(CompletionTrieCache.class);

    private Map<Pattern, CompletionTrie> counts = null;

    public CompletionTrieCache(GlmtkPaths paths) {
        super(paths);
    }

    ////////////////////////////////////////////////////////////////////////////
    // Counts //////////////////////////////////////////////////////////////////

    @Override
    void loadCounts(Collection<Pattern> patterns) throws IOException {
        checkPatternsArg(patterns);

        LOGGER.debug("Loading counts...");

        if (counts == null)
            counts = new HashMap<>();

        for (Pattern pattern : patterns) {
            if (counts.containsKey(pattern))
                continue;

            CompletionTrieBuilder completionTrieBuilder = new CompletionTrieBuilder(
                    true);

            Path file = paths.getPatternsFile(pattern);
            try (CountsReader reader = new CountsReader(file, Constants.CHARSET)) {
                while (reader.readLine() != null)
                    completionTrieBuilder.add(reader.getSequence(),
                            reader.getCount());
            }

            CompletionTrie completionTrie = completionTrieBuilder.build();

            // Free memory
            completionTrieBuilder.reset();
            completionTrieBuilder = null;

            counts.put(pattern, completionTrie);

            if (progress != null)
                progress.increase(1);
        }
    }

    @Override
    public long getCount(NGram ngram) {
        Objects.requireNonNull(ngram);
        if (ngram.isEmpty())
            throw new IllegalArgumentException("Empty ngram.");

        if (counts == null)
            throw new IllegalStateException("Counts not loaded.");

        CompletionTrie completionTrie = counts.get(ngram.getPattern());
        if (completionTrie == null)
            throw new IllegalStateException(
                    "Counts with pattern '%s' not loaded.");

        Long result = completionTrie.get(ngram.toString());
        return result == null ? 0L : result;
    }

    public CompletionTrie getCompletionTrie(Pattern pattern) {
        Objects.requireNonNull(pattern);
        if (pattern.isEmpty())
            throw new IllegalArgumentException("Empty pattern.");
        return counts.get(pattern);
    }

    ////////////////////////////////////////////////////////////////////////////
    // Gammas //////////////////////////////////////////////////////////////////

    @Override
    void loadGammas(Collection<Pattern> patterns) {
        throw new UnsupportedOperationException();
    }

    @Override
    public double getGamma(NGram ngram) {
        throw new UnsupportedOperationException();
    }
}
