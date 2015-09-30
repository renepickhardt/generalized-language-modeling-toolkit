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

import static com.google.common.io.Files.write;
import static de.glmtk.common.Pattern.SKP_PATTERN;
import static de.glmtk.common.PatternElem.WSKP;
import static java.lang.String.format;
import static java.nio.file.Files.createDirectories;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import de.glmtk.Constants;
import de.glmtk.GlmtkPaths;
import de.glmtk.common.NGram;
import de.glmtk.common.Pattern;
import de.glmtk.exceptions.SwitchCaseNotImplementedException;
import de.glmtk.files.CountsReader;
import de.glmtk.logging.Logger;
import de.glmtk.util.completiontrie.CompletionTrie;
import de.glmtk.util.completiontrie.CompletionTrieBuilder;


public class CompletionTrieCache extends AbstractCache {
    private static enum TrieType {
        COUNTS, GAMMAS_HIGH, GAMMAS_LOW
    }

    private static final Logger LOGGER = Logger.get(CompletionTrieCache.class);

    private Map<Pattern, CompletionTrie> counts = null;
    private Map<Pattern, CompletionTrie> gammasHigh = null;
    private Map<Pattern, CompletionTrie> gammasLow = null;

    public CompletionTrieCache(GlmtkPaths paths) {
        super(paths);
    }

    ////////////////////////////////////////////////////////////////////////////
    // Counts //////////////////////////////////////////////////////////////////

    @Override
            /* package */void loadCounts(Collection<Pattern> patterns)
                    throws IOException {
        checkCountPatternsArg(patterns);

        LOGGER.debug("Loading counts for patterns: %s", patterns);

        if (counts == null) {
            counts = new HashMap<>();
        }

        for (Pattern pattern : patterns) {
            if (counts.containsKey(pattern)) {
                continue;
            }
            ensureCompletionTrieMaterialized(pattern, TrieType.COUNTS);

            if (progressBar != null) {
                progressBar.increase();
            }
        }

        for (Pattern pattern : patterns) {
            if (counts.containsKey(pattern)) {
                continue;
            }

            CompletionTrie completionTrie =
                loadCompletionTrieFromTrieFile(pattern, TrieType.COUNTS);

            counts.put(pattern, completionTrie);
        }
    }

    @Override
    public long getCount(NGram ngram) {
        checkNGramArg(ngram);
        checkCountsLoaded();

        CompletionTrie completionTrie = counts.get(ngram.getPattern());
        if (completionTrie == null) {
            throw new IllegalStateException(String.format(
                "Counts with pattern '%s' not loaded.", ngram.getPattern()));
        }

        Long result = completionTrie.get(ngram.toString());
        return result == null ? 0L : result;
    }

    public CompletionTrie getCountCompletionTrie(Pattern pattern) {
        checkPatternArg(pattern);
        checkCountsLoaded();
        return counts.get(pattern);
    }

    private void checkCountsLoaded() {
        if (counts == null) {
            throw new IllegalStateException("Counts not loaded.");
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // Gammas //////////////////////////////////////////////////////////////////

    /**
     * {@link CompletionTrie}s can only store <{@link String}, {@link Long}>
     * pairs. In order to store {@link Double}s, we have to convert doubles to
     * long on a byte level, using {@link #longFromDouble(double)}.
     */
    @Override
            /* package */void loadGammas(Collection<Pattern> patterns)
                    throws IOException {
        checkGammaPatternsArg(patterns);

        LOGGER.debug("Loading gammas for patterns: %s", patterns);

        if (gammasHigh == null) {
            gammasHigh = new HashMap<>();
        }
        if (gammasLow == null) {
            gammasLow = new HashMap<>();
        }

        for (Pattern pattern : patterns) {
            if (gammasHigh.containsKey(pattern)
                && gammasLow.containsKey(pattern)) {
                continue;
            }

            ensureCompletionTrieMaterialized(pattern, TrieType.GAMMAS_HIGH);
            ensureCompletionTrieMaterialized(pattern, TrieType.GAMMAS_LOW);

            if (progressBar != null) {
                progressBar.increase();
            }
        }

        for (Pattern pattern : patterns) {
            if (gammasHigh.containsKey(pattern)) {
                continue;
            }

            CompletionTrie completionTrie =
                loadCompletionTrieFromTrieFile(pattern, TrieType.GAMMAS_HIGH);
            gammasHigh.put(pattern, completionTrie);
        }

        for (Pattern pattern : patterns) {
            if (gammasLow.containsKey(pattern)) {
                continue;
            }

            CompletionTrie completionTrie =
                loadCompletionTrieFromTrieFile(pattern, TrieType.GAMMAS_LOW);
            gammasLow.put(pattern, completionTrie);
        }
    }

    /**
     * {@link CompletionTrie}s can only store <{@link String}, {@link Long}>
     * pairs. In order to load {@link Double}s, we have to convert longs to
     * double on a byte level, using {@link #doubleFromLong(long)}.
     */
    @Override
    public double getGammaHigh(NGram ngram) {
        checkNGramArg(ngram);
        checkGammasLoaded();

        CompletionTrie completionTrie = gammasHigh.get(ngram.getPattern());
        if (completionTrie == null) {
            throw new IllegalStateException(
                format("Higest order Gammas for pattern '%s' not loaded.",
                    ngram.getPattern()));
        }

        Long result = completionTrie.get(ngram.toString());
        return result == null ? 0.0 : doubleFromLong(result);
    }

    @Override
    public double getGammaLow(NGram ngram) {
        checkNGramArg(ngram);
        checkGammasLoaded();

        CompletionTrie completionTrie = gammasLow.get(ngram.getPattern());
        if (completionTrie == null) {
            throw new IllegalStateException(
                format("Lower orders Gammas for pattern '%s' not loaded.",
                    ngram.getPattern()));
        }

        Long result = completionTrie.get(ngram.toString());
        return result == null ? 0.0 : doubleFromLong(result);
    }

    /**
     * Use {@link #doubleFromLong(long)} to convert the {@link Long}s from the
     * {@link CompletionTrie} to {@link Double}s.
     */
    public CompletionTrie getGammasHighCompletionTrie(Pattern pattern) {
        checkPatternArg(pattern);
        checkGammasLoaded();
        return gammasHigh.get(pattern);
    }

    /**
     * Use {@link #doubleFromLong(long)} to convert the {@link Long}s from the
     * {@link CompletionTrie} to {@link Double}s.
     */
    public CompletionTrie getGammasLowCompletionTrie(Pattern pattern) {
        checkPatternArg(pattern);
        checkGammasLoaded();
        return gammasLow.get(pattern);
    }

    private void checkGammasLoaded() {
        if (gammasHigh == null) {
            throw new IllegalStateException("Highest order Gammas not loaded.");
        }
        if (gammasLow == null) {
            throw new IllegalStateException("Lower orders Gamma not loaded.");
        }
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
     * @see #getGammaHigh(NGram)
     * @see #getGammaLow(NGram)
     */
    public static double doubleFromLong(long l) {
        byte[] bytes = new byte[8];
        ByteBuffer.wrap(bytes).putLong(l);
        return ByteBuffer.wrap(bytes).getDouble();
    }

    ////////////////////////////////////////////////////////////////////////////
    // Helper //////////////////////////////////////////////////////////////////

    private void ensureCompletionTrieMaterialized(Pattern pattern,
                                                  TrieType trieType)
                                                          throws IOException {
        Path trieFile;
        switch (trieType) {
            case COUNTS:
                trieFile = paths.getCountsTrieFile(pattern);
                break;

            case GAMMAS_HIGH:
                trieFile = paths.getGammasHighTrieFile(pattern);
                break;

            case GAMMAS_LOW:
                trieFile = paths.getGammasLowTrieFile(pattern);
                break;

            default:
                throw new SwitchCaseNotImplementedException();
        }

        if (Files.exists(trieFile)) {
            return;
        }

        CompletionTrie completionTrie =
            loadCompletionTrieFromRawFile(pattern, trieType);

        createDirectories(paths.getTriesDir());
        write(completionTrie.getMemory(), trieFile.toFile());
    }

    private CompletionTrie loadCompletionTrieFromRawFile(Pattern pattern,
                                                         TrieType trieType)
                                                                 throws IOException {
        CompletionTrieBuilder completionTrieBuilder =
            new CompletionTrieBuilder(true);

        Path rawFile;
        switch (trieType) {
            case COUNTS:
                rawFile = paths.getPatternsFile(pattern);
                break;

            case GAMMAS_HIGH:
                rawFile = paths.getPatternsFile(pattern.concat(WSKP));
                break;

            case GAMMAS_LOW:
                rawFile = paths
                    .getPatternsFile(SKP_PATTERN.concat(pattern).concat(WSKP));
                break;

            default:
                throw new SwitchCaseNotImplementedException();
        }

        try (CountsReader reader =
            new CountsReader(rawFile, Constants.CHARSET)) {
            switch (trieType) {
                case COUNTS:
                    while (reader.readLine() != null) {
                        completionTrieBuilder.add(reader.getSequence(),
                            reader.getCount());
                    }
                    break;

                case GAMMAS_HIGH:
                    while (reader.readLine() != null) {
                        String sequence =
                            getGammaHighNGram(reader.getSequence());
                        double gamma = calcGamma(pattern, reader.getCounts());
                        completionTrieBuilder.add(sequence,
                            longFromDouble(gamma));
                    }
                    break;

                case GAMMAS_LOW:
                    while (reader.readLine() != null) {
                        String sequence =
                            getGammaLowNGram(reader.getSequence());
                        double gamma = calcGamma(pattern, reader.getCounts());
                        completionTrieBuilder.add(sequence,
                            longFromDouble(gamma));
                    }
                    break;

                default:
                    throw new SwitchCaseNotImplementedException();
            }
        }

        CompletionTrie completionTrie = completionTrieBuilder.build();

        // Free memory
        completionTrieBuilder.reset();
        completionTrieBuilder = null;

        return completionTrie;
    }

    private CompletionTrie loadCompletionTrieFromTrieFile(Pattern pattern,
                                                          TrieType trieType)
                                                                  throws IOException {
        Path trieFile;
        switch (trieType) {
            case COUNTS:
                trieFile = paths.getCountsTrieFile(pattern);
                break;

            case GAMMAS_HIGH:
                trieFile = paths.getGammasHighTrieFile(pattern);
                break;

            case GAMMAS_LOW:
                trieFile = paths.getGammasLowTrieFile(pattern);
                break;

            default:
                throw new SwitchCaseNotImplementedException();
        }

        byte[] memory = Files.readAllBytes(trieFile);
        return new CompletionTrie(memory, true);
    }
}
