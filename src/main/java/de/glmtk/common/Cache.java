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

package de.glmtk.common;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    public Cache(GlmtkPaths paths) {
        this.paths = paths;
        progress = null;

        counts = null;
        nGramTimes = null;
        lengthFrequencies = null;
        discounts = null;
        alphas = null;
        lambdas = null;
    }

    public void setProgress(Progress progress) {
        this.progress = progress;
    }

    public void loadCounts(Set<Pattern> patterns) throws IOException {
        LOGGER.debug("Loading counts...");

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
                    String s = reader.getSequence();
                    Counts c = reader.getCounts();
                    countsForPattern.put(s, isPatternAbsolute
                            ? c.getOnePlusCount()
                                    : c);
                }
            }

            progress.increase(1);
        }
    }

    public void loadNGramTimes() throws IOException {
        LOGGER.debug("Loading NGram times counts...");

        nGramTimes = new HashMap<>();
        try (NGramTimesReader reader = new NGramTimesReader(
                paths.getNGramTimesFile(), Constants.CHARSET)) {
            while (reader.readLine() != null) {
                Pattern p = reader.getPattern();
                NGramTimes n = reader.getNGramTimes();
                nGramTimes.put(p, n);
            }
        }

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

        Map<Pattern, Discount> discounts = new HashMap<>();
        this.discounts.put(model, discounts);

        try (DiscountReader reader = new DiscountReader(file, Constants.CHARSET)) {
            while (reader.readLine() != null) {
                Pattern p = reader.getPattern();
                Discount d = reader.getDiscount();
                discounts.put(p, d);
            }
        }
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

        Map<Pattern, Map<String, AlphaCount>> alphasForModel = new HashMap<>();
        alphas.put(model, alphasForModel);

        for (Pattern pattern : patterns) {
            Map<String, AlphaCount> alphasForPattern = new HashMap<>();
            alphasForModel.put(pattern, alphasForPattern);

            Path file = inputDir.resolve(pattern.toString());
            try (AlphaCountReader reader = new AlphaCountReader(file,
                    Constants.CHARSET)) {
                while (reader.readLine() != null) {
                    String s = reader.getSequence();
                    AlphaCount a = reader.getAlphaCounts();
                    alphasForPattern.put(s, a);
                }
            }

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

        Map<Pattern, Map<String, LambdaCounts>> lambdasForModel = new HashMap<>();
        lambdas.put(model, lambdasForModel);

        for (Pattern pattern : patterns) {
            Map<String, LambdaCounts> lambdasForPattern = new HashMap<>();
            lambdasForModel.put(pattern, lambdasForPattern);

            Path file = inputDir.resolve(pattern.toString());
            try (LambdaCountsReader reader = new LambdaCountsReader(file,
                    Constants.CHARSET)) {
                while (reader.readLine() != null) {
                    String s = reader.getSequence();
                    LambdaCounts l = reader.getLambdaCounts();
                    lambdasForPattern.put(s, l);
                }
            }

            progress.increase(1);
        }
    }
}
