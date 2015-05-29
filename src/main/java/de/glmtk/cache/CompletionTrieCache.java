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
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import de.glmtk.Constants;
import de.glmtk.GlmtkPaths;
import de.glmtk.common.NGram;
import de.glmtk.common.Pattern;
import de.glmtk.common.PatternElem;
import de.glmtk.files.CountsReader;
import de.glmtk.logging.Logger;
import de.glmtk.util.completiontrie.CompletionTrie;
import de.glmtk.util.completiontrie.CompletionTrieBuilder;

public class CompletionTrieCache extends AbstractCache {
    private static final Logger LOGGER = Logger.get(CompletionTrieCache.class);

    private Map<Pattern, CompletionTrie> counts = null;
    private Map<Pattern, CompletionTrie> gammas = null;

    public CompletionTrieCache(GlmtkPaths paths) {
        super(paths);
    }

    ////////////////////////////////////////////////////////////////////////////
    // Counts //////////////////////////////////////////////////////////////////

    @Override
    void loadCounts(Collection<Pattern> patterns) throws IOException {
        checkCountPatternsArg(patterns);

        LOGGER.debug("Loading counts for patterns: %s", patterns);

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

            if (progressBar != null)
                progressBar.increase();
        }
    }

    @Override
    public long getCount(NGram ngram) {
        checkNGramArg(ngram);
        checkCountsLoaded();

        CompletionTrie completionTrie = counts.get(ngram.getPattern());
        if (completionTrie == null)
            throw new IllegalStateException(String.format(
                    "Counts with pattern '%s' not loaded.", ngram.getPattern()));

        Long result = completionTrie.get(ngram.toString());
        return result == null ? 0L : result;
    }

    public CompletionTrie getCountCompletionTrie(Pattern pattern) {
        checkPatternArg(pattern);
        checkCountsLoaded();
        return counts.get(pattern);
    }

    private void checkCountsLoaded() {
        if (counts == null)
            throw new IllegalStateException("Counts not loaded.");
    }

    ////////////////////////////////////////////////////////////////////////////
    // Gammas //////////////////////////////////////////////////////////////////

    /**
     * {@link CompletionTrie}s can only store <{@link String}, {@link Long}>
     * pairs. In order to store {@link Double}s, we have to convert doubles to
     * long on a byte level, using {@link #longFromDouble(double)}.
     */
    @Override
    void loadGammas(Collection<Pattern> patterns) throws IOException {
        checkGammaPatternsArg(patterns);

        LOGGER.debug("Loading gammas for patterns: %s", patterns);

        if (gammas == null)
            gammas = new HashMap<>();

        for (Pattern pattern : patterns) {
            if (gammas.containsKey(pattern))
                continue;

            CompletionTrieBuilder completionTrieBuilder = new CompletionTrieBuilder(
                    true);

            Path file = paths.getPatternsFile(pattern.concat(PatternElem.WSKP));
            try (CountsReader reader = new CountsReader(file, Constants.CHARSET)) {
                while (reader.readLine() != null)
                    completionTrieBuilder.add(
                            removeTrailingWSkp(reader.getSequence()),
                            longFromDouble(calcGamma(pattern,
                                    reader.getCounts())));
            }

            CompletionTrie completionTrie = completionTrieBuilder.build();

            // Free memory
            completionTrieBuilder.reset();
            completionTrieBuilder = null;

            gammas.put(pattern, completionTrie);

            if (progressBar != null)
                progressBar.increase();
        }
    }

    /**
     * {@link CompletionTrie}s can only store <{@link String}, {@link Long}>
     * pairs. In order to load {@link Double}s, we have to convert longs to
     * double on a byte level, using {@link #doubleFromLong(long)}.
     */
    @Override
    public double getGamma(NGram ngram) {
        checkNGramArg(ngram);
        checkGammasLoaded();

        CompletionTrie completionTrie = gammas.get(ngram.getPattern());
        if (completionTrie == null)
            throw new IllegalStateException(String.format(
                    "Gammas with pattern '%s' not loaded.", ngram.getPattern()));

        Long result = completionTrie.get(ngram.toString());
        return result == null ? 0.0 : doubleFromLong(result);
    }

    /**
     * Use {@link #doubleFromLong(long)} to convert the {@link Long}s from the
     * {@link CompletionTrie} to {@link Double}s.
     */
    public CompletionTrie getGammaCompletionTrie(Pattern pattern) {
        checkPatternArg(pattern);
        checkGammasLoaded();
        return gammas.get(pattern);
    }

    private void checkGammasLoaded() {
        if (gammas == null)
            throw new IllegalStateException("Gammas not loaded.");
    }

    /**
     * @see #loadGammas(Collection)
     */
    private static long longFromDouble(double d) {
        byte[] bytes = new byte[8];
        ByteBuffer.wrap(bytes).putDouble(d);
        return ByteBuffer.wrap(bytes).getLong();
    }

    /**
     * @see #getGamma(NGram)
     */
    public static double doubleFromLong(long l) {
        byte[] bytes = new byte[8];
        ByteBuffer.wrap(bytes).putLong(l);
        return ByteBuffer.wrap(bytes).getDouble();
    }
}
