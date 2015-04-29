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

package de.glmtk.cache;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

import de.glmtk.Constants;
import de.glmtk.GlmtkPaths;
import de.glmtk.common.NGram;
import de.glmtk.common.Output.Progress;
import de.glmtk.common.Pattern;
import de.glmtk.common.PatternElem;
import de.glmtk.common.Patterns;
import de.glmtk.counts.Counts;
import de.glmtk.counts.NGramTimes;
import de.glmtk.files.CountsReader;
import de.glmtk.files.LengthDistributionReader;
import de.glmtk.files.NGramTimesReader;
import de.glmtk.logging.Logger;
import de.glmtk.util.CollectionUtils;
import de.glmtk.util.completiontrie.CompletionTrie;
import de.glmtk.util.completiontrie.CompletionTrieBuilder;

/**
 * Use {@link CacheBuilder} for creation.
 */
public class Cache {
    private static final Logger LOGGER = Logger.get(Cache.class);

    private GlmtkPaths paths;
    private Progress progress;

    /**
     * Stores either {@link Long} for absolute patterns or {@link Counts} for
     * continuation patterns;
     */
    private Map<Pattern, Map<String, Object>> counts;
    private Map<Pattern, CompletionTrie> completionCounts;
    private Map<Pattern, NGramTimes> ngramTimes;
    private List<Double> lengthFrequencies;

    private long numWords;
    private long vocabSize;
    private SortedSet<String> words;

    public Cache(GlmtkPaths paths) {
        this.paths = paths;
        progress = null;

        counts = null;
        completionCounts = null;
        ngramTimes = null;
        lengthFrequencies = null;

        numWords = -1L;
        vocabSize = -1L;
        words = null;
    }

    public void setProgress(Progress progress) {
        this.progress = progress;
    }

    public void loadCounts(Collection<Pattern> patterns) throws IOException {
        Objects.requireNonNull(patterns);
        for (Pattern pattern : patterns)
            if (pattern.isEmpty())
                throw new IllegalArgumentException(
                        "patterns contains empty pattern.");

        LOGGER.debug("Loading counts...");

        if (counts == null)
            counts = new HashMap<>();

        for (Pattern pattern : patterns) {
            if (counts.containsKey(pattern))
                continue;

            boolean isPatternAbsolute = pattern.isAbsolute();
            Path inputDir = (isPatternAbsolute
                    ? paths.getAbsoluteDir()
                    : paths.getContinuationDir());

            Map<String, Object> countsForPattern = new HashMap<>();
            counts.put(pattern, countsForPattern);

            Path file = inputDir.resolve(pattern.toString());
            try (CountsReader reader = new CountsReader(file, Constants.CHARSET)) {
                while (reader.readLine() != null) {
                    Counts c = reader.getCounts();
                    Object value = isPatternAbsolute ? c.getOnePlusCount() : c;
                    countsForPattern.put(reader.getSequence(), value);
                }
            }

            if (progress != null)
                progress.increase(1);
        }
    }

    public void loadCompletionCounts(Collection<Pattern> patterns) throws IOException {
        Objects.requireNonNull(patterns);
        for (Pattern pattern : patterns)
            if (pattern.isEmpty())
                throw new IllegalArgumentException(
                        "patterns contains empty pattern.");

        LOGGER.debug("Loading completion counts...");

        if (completionCounts == null)
            completionCounts = new HashMap<>();

        for (Pattern pattern : patterns) {
            if (completionCounts.containsKey(pattern))
                continue;

            boolean isPatternAbsolute = pattern.isAbsolute();
            Path inputDir = (isPatternAbsolute
                    ? paths.getAbsoluteDir()
                    : paths.getContinuationDir());

            CompletionTrieBuilder completionTrieBuilder = new CompletionTrieBuilder(
                    true);

            Path file = inputDir.resolve(pattern.toString());
            try (CountsReader reader = new CountsReader(file, Constants.CHARSET)) {
                while (reader.readLine() != null)
                    completionTrieBuilder.add(reader.getSequence(),
                            reader.getCount());
            }

            CompletionTrie completionTrie = completionTrieBuilder.build();

            // Free memory
            completionTrieBuilder.reset();
            completionTrieBuilder = null;

            completionCounts.put(pattern, completionTrie);

            if (progress != null)
                progress.increase(1);
        }
    }

    public void loadNGramTimes() throws IOException {
        LOGGER.debug("Loading NGram times counts...");

        ngramTimes = new HashMap<>();
        try (NGramTimesReader reader = new NGramTimesReader(
                paths.getNGramTimesFile(), Constants.CHARSET)) {
            while (reader.readLine() != null)
                ngramTimes.put(reader.getPattern(), reader.getNGramTimes());
        }

        if (progress != null)
            progress.increase(1);
    }

    public void loadLengthDistribution() throws IOException {
        LOGGER.debug("Loading Length Distribution...");

        lengthFrequencies = new ArrayList<>();
        try (LengthDistributionReader reader = new LengthDistributionReader(
                paths.getLengthDistributionFile(), Constants.CHARSET)) {
            while (reader.readLine() != null) {
                int l = reader.getLength();
                double f = reader.getFrequency();

                CollectionUtils.fill(lengthFrequencies, l, 0.0);
                lengthFrequencies.set(l, f);
            }
        }

        if (progress != null)
            progress.increase(1);
    }

    public long getCount(NGram ngram) {
        Objects.requireNonNull(ngram);
        if (ngram.isEmpty())
            throw new IllegalArgumentException("Empty ngram.");
        Pattern pattern = ngram.getPattern();

        if (pattern.isAbsolute()) {
            Long result = (Long) CollectionUtils.getFromNestedMap(counts,
                    pattern, ngram.toString(), "Counts not loaded",
                    "Counts with pattern '%s' not loaded.", null);
            return result == null ? 0 : result;
        }

        Counts result = (Counts) CollectionUtils.getFromNestedMap(counts,
                pattern, ngram.toString(), "Counts not loaded",
                "Counts with pattern '%s' not loaded.", null);
        return result == null ? 0 : result.getOnePlusCount();
    }

    /**
     * Use {@link #getCount(NGram)} instead.
     */
    @Deprecated
    public long getAbsolute(NGram ngram) {
        Objects.requireNonNull(ngram);
        if (ngram.isEmpty())
            throw new IllegalArgumentException("Empty ngram.");
        Pattern pattern = ngram.getPattern();
        if (!pattern.isAbsolute())
            throw new IllegalArgumentException(String.format(
                    "Pattern '%s' is no absolute pattern.", pattern));

        Long result = (Long) CollectionUtils.getFromNestedMap(counts, pattern,
                ngram.toString(), "Counts not loaded",
                "Counts with pattern '%s' not loaded.", null);

        return result == null ? 0L : result;
    }

    public Counts getContinuation(NGram ngram) {
        Objects.requireNonNull(ngram);
        if (ngram.isEmpty())
            throw new IllegalArgumentException("Empty ngram.");
        Pattern pattern = ngram.getPattern();
        if (pattern.isAbsolute())
            throw new IllegalArgumentException(String.format(
                    "Pattern '%s' is no continuation pattern.", pattern));

        Counts result = (Counts) CollectionUtils.getFromNestedMap(counts,
                pattern, ngram.toString(), "Counts not loaded",
                "Counts with pattern '%s' not loaded.", null);

        return result == null ? new Counts() : result;
    }

    public CompletionTrie getCompletionCounts(Pattern pattern) {
        Objects.requireNonNull(pattern);
        if (pattern.isEmpty())
            throw new IllegalArgumentException("Empty pattern.");

        return completionCounts.get(pattern);
    }

    public long getNumWords() {
        if (numWords == -1)
            numWords = getCount(NGram.SKP_NGRAM);
        return numWords;
    }

    public long getVocabSize() {
        if (vocabSize == -1)
            vocabSize = getCount(NGram.WSKP_NGRAM);
        return vocabSize;
    }

    public SortedSet<String> getWords() {
        if (words == null)
            words = new TreeSet<>(
                    counts.get(Patterns.get(PatternElem.CNT)).keySet());
        return words;
    }

    public NGramTimes getNGramTimes(Pattern pattern) {
        Objects.requireNonNull(pattern);
        if (pattern.isEmpty())
            throw new IllegalArgumentException("Empty pattern.");
        return CollectionUtils.getFromNestedMap(ngramTimes, pattern,
                "NGram times not loaded.",
                "No NGramTimes learned for pattern '%s'.");
    }

    public double getLengthFrequency(int length) {
        if (lengthFrequencies == null)
            throw new IllegalStateException("Length distribution not loaded.");

        if (length < 1)
            throw new IllegalArgumentException(
                    String.format(
                            "Illegal length requested: '%d'. Must be an integer greater zero.",
                            length));

        if (length >= lengthFrequencies.size())
            return 0.0;

        return lengthFrequencies.get(length);
    }

    public int getMaxSequenceLength() {
        if (lengthFrequencies == null)
            throw new IllegalStateException("Length distribution not loaded.");

        return lengthFrequencies.size();
    }
}
