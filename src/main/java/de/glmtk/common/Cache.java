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

package de.glmtk.common;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import de.glmtk.Constants;
import de.glmtk.GlmtkPaths;
import de.glmtk.common.Output.Progress;
import de.glmtk.counts.AlphaCount;
import de.glmtk.counts.Counts;
import de.glmtk.counts.Discount;
import de.glmtk.counts.LambdaCounts;
import de.glmtk.counts.NGramTimes;
import de.glmtk.files.AlphaCountReader;
import de.glmtk.files.CountsReader;
import de.glmtk.files.DiscountReader;
import de.glmtk.files.LambdaCountsReader;
import de.glmtk.files.LengthDistributionReader;
import de.glmtk.files.NGramTimesReader;
import de.glmtk.logging.Logger;
import de.glmtk.util.CollectionUtils;

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
    private Map<Pattern, NGramTimes> nGramTimes;
    private List<Double> lengthFrequencies;
    private Map<String, Map<Pattern, Discount>> discounts;
    private Map<String, Map<Pattern, Map<String, AlphaCount>>> alphas;
    private Map<String, Map<Pattern, Map<String, LambdaCounts>>> lambdas;

    private long numWords;
    private long vocabSize;
    private SortedSet<String> words;

    public Cache(GlmtkPaths paths) {
        this.paths = paths;
        progress = null;

        counts = null;
        nGramTimes = null;
        lengthFrequencies = null;
        discounts = null;
        alphas = null;
        lambdas = null;

        numWords = -1L;
        vocabSize = -1L;
        words = null;
    }

    public void setProgress(Progress progress) {
        this.progress = progress;
    }

    public void loadCounts(Set<Pattern> patterns) throws IOException {
        LOGGER.debug("Loading counts...");

        if (counts == null)
            counts = new HashMap<>();

        for (Pattern pattern : patterns) {
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

    public void loadNGramTimes() throws IOException {
        LOGGER.debug("Loading NGram times counts...");

        nGramTimes = new HashMap<>();
        try (NGramTimesReader reader = new NGramTimesReader(
                paths.getNGramTimesFile(), Constants.CHARSET)) {
            while (reader.readLine() != null)
                nGramTimes.put(reader.getPattern(), reader.getNGramTimes());
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

                CollectionUtils.ensureListSize(lengthFrequencies, l, 0.0);
                lengthFrequencies.set(l, f);
            }
        }

        if (progress != null)
            progress.increase(1);
    }

    public void loadDiscounts(String model) throws IOException {
        LOGGER.debug("Loading %s discounts...", model);

        Path file = null;
        if (model.equals(Constants.MODEL_MODKNESERNEY_NAME))
            file = paths.getModKneserNeyDiscountsFile();
        else
            throw new IllegalArgumentException(String.format(
                    "Illegal model '%s'.", model));

        if (discounts == null)
            discounts = new HashMap<>();

        Map<Pattern, Discount> discountsForModel = new HashMap<>();
        discounts.put(model, discountsForModel);

        try (DiscountReader reader = new DiscountReader(file, Constants.CHARSET)) {
            while (reader.readLine() != null)
                discountsForModel.put(reader.getPattern(), reader.getDiscount());
        }

        if (progress != null)
            progress.increase(1);
    }

    public void loadAlphas(String model,
                           Set<Pattern> patterns) throws IOException {
        LOGGER.debug("Loading %s alphas...", model);

        Path inputDir = null;
        if (model.equals(Constants.MODEL_MODKNESERNEY_NAME))
            inputDir = paths.getModKneserNeyAlphaDir();
        else
            throw new IllegalArgumentException(String.format(String.format(
                    "Illegal model '%s'.", model)));

        if (alphas == null)
            alphas = new HashMap<>();

        Map<Pattern, Map<String, AlphaCount>> alphasForModel = new HashMap<>();
        alphas.put(model, alphasForModel);

        for (Pattern pattern : patterns) {
            Map<String, AlphaCount> alphasForPattern = new HashMap<>();
            alphasForModel.put(pattern, alphasForPattern);

            Path file = inputDir.resolve(pattern.toString());
            try (AlphaCountReader reader = new AlphaCountReader(file,
                    Constants.CHARSET)) {
                while (reader.readLine() != null)
                    alphasForPattern.put(reader.getSequence(),
                            reader.getAlphaCounts());
            }

            if (progress != null)
                progress.increase(1);
        }
    }

    public void loadLambdas(String model,
                            Set<Pattern> patterns) throws IOException {
        LOGGER.debug("Loading %s lambdas...", model);

        Path inputDir = null;
        if (model.equals(Constants.MODEL_MODKNESERNEY_NAME))
            inputDir = paths.getModKneserNeyLambdaDir();
        else
            throw new IllegalArgumentException(String.format(String.format(
                    "Illegal model '%s'.", model)));

        if (lambdas == null)
            lambdas = new HashMap<>();

        Map<Pattern, Map<String, LambdaCounts>> lambdasForModel = new HashMap<>();
        lambdas.put(model, lambdasForModel);

        for (Pattern pattern : patterns) {
            Map<String, LambdaCounts> lambdasForPattern = new HashMap<>();
            lambdasForModel.put(pattern, lambdasForPattern);

            Path file = inputDir.resolve(pattern.toString());
            try (LambdaCountsReader reader = new LambdaCountsReader(file,
                    Constants.CHARSET)) {
                while (reader.readLine() != null)
                    lambdasForPattern.put(reader.getSequence(),
                            reader.getLambdaCounts());
            }

            if (progress != null)
                progress.increase(1);
        }
    }

    public long getAbsolute(NGram ngram) {
        Pattern pattern = ngram.getPattern();
        if (!pattern.isAbsolute())
            throw new IllegalArgumentException(String.format(
                    "Pattern '%s' is is not absolute pattern.", pattern));

        Long result = (Long) CollectionUtils.getFromNestedMap(counts, pattern,
                ngram.toString(), "Counts not loaded",
                "Counts with pattern '%s' not loaded.", null);

        return result == null ? 0L : result;
    }

    public Counts getContinuation(NGram ngram) {
        Pattern pattern = ngram.getPattern();
        if (pattern.isAbsolute())
            throw new IllegalArgumentException(String.format(
                    "Pattern '%s' is no continuation pattern.", pattern));

        Counts result = (Counts) CollectionUtils.getFromNestedMap(counts,
                pattern, ngram.toString(), "Counts not loaded",
                "Counts with pattern '%s' not loaded.", null);

        return result == null ? new Counts() : result;
    }

    public long getNumWords() {
        if (numWords == -1)
            numWords = getAbsolute(NGram.SKP_NGRAM);
        return numWords;
    }

    public long getVocabSize() {
        if (vocabSize == -1)
            vocabSize = getContinuation(NGram.WSKP_NGRAM).getOnePlusCount();
        return vocabSize;
    }

    public SortedSet<String> getWords() {
        if (words == null)
            words = new TreeSet<>(
                    counts.get(Patterns.get(PatternElem.CNT)).keySet());
        return words;
    }

    public NGramTimes getNGramTimes(Pattern pattern) {
        if (nGramTimes == null)
            throw new IllegalStateException("NGram times not loaded.");

        return CollectionUtils.getFromNestedMap(nGramTimes, pattern,
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

    public Discount getDiscount(String model,
                                Pattern pattern) {
        return CollectionUtils.getFromNestedMap(discounts, model, pattern,
                "No Discounts loaded.", "Discounts for model '%s' not loaded.",
                "No Discounts learned for pattern '%2$s' for model '%1$s'.");
    }

    public AlphaCount getAlpha(String model,
                               NGram ngram) {
        return CollectionUtils.getFromNestedMap(alphas, model,
                ngram.getPattern(), ngram.toString(), "No alphas loaded.",
                "Alphas for model '%s' not loaded.",
                "Alphas with pattern '%2$s' for model '%1$s' not loaded.", null);
    }

    public LambdaCounts getLambda(String model,
                                  NGram ngram) {
        return CollectionUtils.getFromNestedMap(lambdas, model,
                ngram.getPattern(), ngram.toString(), "No lambdas loaded",
                "Lambdas for model '%s' not loaded.",
                "Lambdas with pattern '%2$s' for model '%1$s' not loaded.",
                null);
    }
}