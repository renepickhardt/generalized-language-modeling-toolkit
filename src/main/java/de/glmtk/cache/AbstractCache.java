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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import de.glmtk.Constants;
import de.glmtk.GlmtkPaths;
import de.glmtk.common.NGram;
import de.glmtk.common.Output.Progress;
import de.glmtk.common.Pattern;
import de.glmtk.common.PatternElem;
import de.glmtk.common.Patterns;
import de.glmtk.counts.NGramTimes;
import de.glmtk.files.CountsReader;
import de.glmtk.files.LengthDistributionReader;
import de.glmtk.files.NGramTimesReader;
import de.glmtk.logging.Logger;
import de.glmtk.util.CollectionUtils;

/**
 * Implements functionality that is common to all cache implementations.
 *
 * Implements count functionalities {@link #getNumWords()} and
 * {@link #getVocabSize()} as these are simple convenience methods, that do not
 * need information on how counts are stored.
 *
 * Implements Words, NGramTimes and LengthDistribution functionality.
 */
public abstract class AbstractCache implements Cache {
    private static final Logger LOGGER = Logger.get(AbstractCache.class);

    protected GlmtkPaths paths;
    protected Progress progress = null;

    private long numWords = -1L;
    private long vocabSize = -1L;
    private SortedSet<String> words = null;

    private Map<Pattern, NGramTimes> ngramTimes = null;
    private List<Double> lengthFrequencies = null;

    public AbstractCache(GlmtkPaths paths) {
        this.paths = paths;
    }

    /* package */void setProgress(Progress progress) {
        this.progress = progress;
    }

    protected void checkPatternsArg(Collection<Pattern> patterns) {
        Objects.requireNonNull(patterns);
        for (Pattern pattern : patterns)
            if (pattern.isEmpty())
                throw new IllegalArgumentException(
                        "Argument 'patterns' contains empty pattern.");
    }

    ////////////////////////////////////////////////////////////////////////////
    // Counts //////////////////////////////////////////////////////////////////

    /* package */abstract void loadCounts(Collection<Pattern> patterns) throws IOException;

    @Override
    public long getNumWords() {
        if (numWords == -1)
            numWords = getCount(NGram.SKP_NGRAM);
        return numWords;
    }

    @Override
    public long getVocabSize() {
        if (vocabSize == -1)
            vocabSize = getCount(NGram.WSKP_NGRAM);
        return vocabSize;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Gammas //////////////////////////////////////////////////////////////////

    /* pacakge */abstract void loadGammas(Collection<Pattern> patterns);

    ////////////////////////////////////////////////////////////////////////////
    // Words ///////////////////////////////////////////////////////////////////

    /* package */void loadWords() throws IOException {
        LOGGER.debug("Loading words...");

        Pattern pattern = Patterns.get(PatternElem.CNT);
        Path file = paths.getAbsoluteDir().resolve(pattern.toString());

        Set<String> unsortedWords = new TreeSet<>();
        try (CountsReader reader = new CountsReader(file, Constants.CHARSET)) {
            while (reader.readLine() != null)
                unsortedWords.add(reader.getSequence());
        }

        words = new TreeSet<>(unsortedWords);

        if (progress != null)
            progress.increase(1);
    }

    @Override
    public SortedSet<String> getWords() {
        checkWordsLoaded();
        return words;
    }

    private void checkWordsLoaded() {
        if (words == null)
            throw new IllegalStateException("Words not loaded.");
    }

    ////////////////////////////////////////////////////////////////////////////
    // NGramTimes //////////////////////////////////////////////////////////////

    /* package */void loadNGramTimes() throws IOException {
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

    @Override
    public NGramTimes getNGramTimes(Pattern pattern) {
        Objects.requireNonNull(pattern);
        if (pattern.isEmpty())
            throw new IllegalArgumentException("Empty pattern.");

        checkNGramTimesLoaded();
        NGramTimes result = ngramTimes.get(pattern);
        if (result == null)
            throw new IllegalStateException(String.format(
                    "No NGramTimes learned for pattern '%s'.", pattern));
        return result;
    }

    private void checkNGramTimesLoaded() {
        if (ngramTimes == null)
            throw new IllegalStateException("NGram times not loaded.");
    }

    ////////////////////////////////////////////////////////////////////////////
    // LengthDistribution //////////////////////////////////////////////////////

    /* package */void loadLengthDistribution() throws IOException {
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

    @Override
    public double getLengthFrequency(int length) {
        if (length <= 0)
            throw new IllegalArgumentException(
                    String.format(
                            "Illegal length requested: '%s'. Must be an integer greater zero.",
                            length));

        checkLengthFrequenciesLoaded();
        if (length >= lengthFrequencies.size())
            return 0.0;
        return lengthFrequencies.get(length);
    }

    @Override
    public int getMaxSequenceLength() {
        checkLengthFrequenciesLoaded();
        return lengthFrequencies.size();
    }

    private void checkLengthFrequenciesLoaded() {
        if (lengthFrequencies == null)
            throw new IllegalStateException("Length distribution not loaded.");
    }
}
