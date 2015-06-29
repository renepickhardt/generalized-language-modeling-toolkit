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

import static de.glmtk.util.NioUtils.CheckFile.EXISTS;

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
import de.glmtk.common.Pattern;
import de.glmtk.common.PatternElem;
import de.glmtk.common.Patterns;
import de.glmtk.counts.Counts;
import de.glmtk.counts.Discounts;
import de.glmtk.counts.NGramTimes;
import de.glmtk.files.CountsReader;
import de.glmtk.files.LengthDistributionReader;
import de.glmtk.files.NGramTimesReader;
import de.glmtk.logging.Logger;
import de.glmtk.output.ProgressBar;
import de.glmtk.util.CollectionUtils;
import de.glmtk.util.NioUtils;

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
    // TODO: Test case that checks for all estimators whether probabilities are equal using different cache implementations.

    private static final Logger LOGGER = Logger.get(AbstractCache.class);

    protected GlmtkPaths paths;
    protected ProgressBar progressBar = null;

    private SortedSet<String> words = null;
    private Map<Pattern, NGramTimes> ngramTimes = null;
    private Map<Pattern, Discounts> discounts = new HashMap<>();
    private List<Double> lengthFrequencies = null;
    private long numWords = -1L;
    private long vocabSize = -1L;

    public AbstractCache(GlmtkPaths paths) {
        this.paths = paths;
    }

    /* package */void setProgressBar(ProgressBar progressBar) {
        this.progressBar = progressBar;
    }

    protected void checkNGramArg(NGram ngram) {
        Objects.requireNonNull(ngram);
        if (ngram.isEmpty())
            throw new IllegalArgumentException(
                    "Argumnet 'ngram' is the empty ngram.");
    }

    protected void checkPatternArg(Pattern pattern) {
        Objects.requireNonNull(pattern);
        if (pattern.isEmpty())
            throw new IllegalArgumentException(
                    "Argument 'pattern' is the empty pattern.");
    }

    protected void checkCountPatternsArg(Collection<Pattern> patterns) {
        Objects.requireNonNull(patterns);
        for (Pattern pattern : patterns)
            if (pattern.isEmpty())
                throw new IllegalArgumentException(
                        "Argument 'patterns' contains empty pattern.");
    }

    protected void checkGammaPatternsArg(Collection<Pattern> patterns) {
        checkCountPatternsArg(patterns);
        for (Pattern pattern : patterns) {
            Path cntFile = paths.getPatternsFile(pattern.concat(PatternElem.CNT));
            Path wskpFile = paths.getPatternsFile(pattern.concat(PatternElem.WSKP));
            if (!NioUtils.checkFile(cntFile, EXISTS)
                    || !NioUtils.checkFile(wskpFile, EXISTS))
                throw new IllegalStateException(
                        String.format(
                                "In order to load gamma counts for pattern '%1$s', counts for patterns '%1$s1' and '%1$sx' have to be computed.",
                                pattern));
        }
    }

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

        if (progressBar != null)
            progressBar.increase();
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
    // Discounts //////////////////////////////////////////////////////////////

    /**
     * Loads discounts and ngramTimes counts.
     */
    /* package */void loadDiscounts() throws IOException {
        LOGGER.debug("Loading Discounts...");

        ngramTimes = new HashMap<>();
        try (NGramTimesReader reader = new NGramTimesReader(
                paths.getNGramTimesFile(), Constants.CHARSET)) {
            while (reader.readLine() != null) {
                Pattern pattern = reader.getPattern();
                NGramTimes times = reader.getNGramTimes();

                ngramTimes.put(pattern, times);
                discounts.put(pattern, calcDiscounts(times));
            }
        }

        if (progressBar != null)
            progressBar.increase();
    }

    private Discounts calcDiscounts(NGramTimes n) {
        double y = (double) n.getOneCount()
                / (n.getOneCount() + n.getTwoCount());
        return new Discounts(1.0f - 2.0f * y * n.getTwoCount()
                / n.getOneCount(), 2.0f - 3.0f * y * n.getThreeCount()
                / n.getTwoCount(), 3.0f - 4.0f * y * n.getFourCount()
                / n.getThreeCount());
    }

    @Override
    public double getDiscount(NGram ngram) {
        checkNGramArg(ngram);
        checkDiscountsLoaded();

        return getDiscounts(ngram.getPattern()).getForCount(getCount(ngram));
    }

    @Override
    public Discounts getDiscounts(Pattern pattern) {
        checkPatternArg(pattern);
        checkDiscountsLoaded();

        Discounts result = discounts.get(pattern);
        if (result == null)
            throw new IllegalArgumentException(String.format(
                    "No Discounts learned for pattern '%s'.", pattern));
        return result;
    }

    private void checkDiscountsLoaded() {
        if (discounts == null)
            throw new IllegalStateException("Discounts not loaded.");
    }

    @Override
    public NGramTimes getNGramTimes(Pattern pattern) {
        checkPatternArg(pattern);
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

        if (progressBar != null)
            progressBar.increase();
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

    /* pacakge */abstract void loadGammas(Collection<Pattern> patterns) throws IOException;

    /**
     * All gamma sequences have a trailing " %" because they all end in WSKP. No
     * need to store that for all gamma sequences.
     */
    protected String removeTrailingWSkp(String sequence) {
        return sequence.substring(0, sequence.length() - 2);
    }

    protected double calcGamma(Pattern pattern,
                               Counts contCount) {
        Discounts discount = getDiscounts(pattern.concat(PatternElem.CNT));
        return discount.getOne() * contCount.getOnePlusCount()
                + discount.getTwo() * contCount.getTwoCount()
                + discount.getThree() + contCount.getThreePlusCount();
    }
}
