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

package de.glmtk.learning.modkneserney;

import static de.glmtk.Constants.MODEL_MODKNESERNEY;
import static de.glmtk.common.NGram.WSKP_NGRAM;
import static de.glmtk.common.Output.OUTPUT;
import static de.glmtk.common.Pattern.WSKP_PATTERN;
import static de.glmtk.common.PatternElem.CNT;
import static de.glmtk.common.PatternElem.SKP;
import static de.glmtk.common.PatternElem.SKP_WORD;
import static de.glmtk.common.PatternElem.WSKP;
import static de.glmtk.common.PatternElem.WSKP_WORD;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.glmtk.Constants;
import de.glmtk.GlmtkPaths;
import de.glmtk.cache.Cache;
import de.glmtk.cache.CacheBuilder;
import de.glmtk.common.AbstractWorkerExecutor;
import de.glmtk.common.BackoffMode;
import de.glmtk.common.Config;
import de.glmtk.common.NGram;
import de.glmtk.common.Output.Phase;
import de.glmtk.common.Pattern;
import de.glmtk.common.Patterns;
import de.glmtk.common.Status;
import de.glmtk.counts.AlphaCounts;
import de.glmtk.counts.Discount;
import de.glmtk.files.AlphaCountsWriter;
import de.glmtk.files.SequenceReader;
import de.glmtk.util.StringUtils;

public class AlphaCalculator extends AbstractWorkerExecutor<Pattern> {
    protected class Worker extends AbstractWorkerExecutor<Pattern>.Worker {
        @Override
        protected void work(Pattern pattern,
                            int patternNo) throws IOException {
            Path sequenceFile = absoluteDir.resolve(pattern.toString());
            Path alphaFile = alphaDir.resolve(pattern.toString());
            try (SequenceReader reader = new SequenceReader(sequenceFile,
                    Constants.CHARSET);
                    AlphaCountsWriter writer = new AlphaCountsWriter(alphaFile,
                            Constants.CHARSET)) {
                while (reader.readLine() != null) {
                    String sequenceString = reader.getSequence();

                    NGram sequence = new NGram(StringUtils.split(
                            sequenceString, ' '));
                    int sequenceOrder = sequence.size();

                    AlphaCounts alphas = new AlphaCounts();
                    alphas.append(calcAlpha(sequence, true, true));
                    alphas.append(calcAlpha(sequence, true,
                            sequenceOrder - 1 == 0));
                    for (int i = 0; i != sequenceOrder; ++i) {
                        double alpha = calcAlpha(sequence, false,
                                sequenceOrder - 1 == i);
                        alphas.append(alpha);
                        sequence = sequence.backoff(BackoffMode.DEL);
                    }

                    writer.append(sequenceString, alphas);
                }
            }

            status.addAlpha(model, pattern);
        }

        /**
         * @param highestOrder
         *            If {@code true} alpha absolute counts, if {@code false}
         *            alpha with continuation counts.
         * @param lowestOrder
         *            If {@code false} discount alpha if {@code true} do not.
         */
        protected double calcAlpha(NGram sequence,
                                   boolean highestOrder,
                                   boolean lowestOrder) {
            double numerator, denominator;
            if (highestOrder) {
                numerator = cache.getAbsolute(sequence);
                denominator = cache.getAbsolute(sequence.set(
                        sequence.size() - 1, SKP_WORD));
            } else {
                numerator = cache.getContinuation(WSKP_NGRAM.concat(sequence)).getOnePlusCount();
                denominator = cache.getContinuation(
                        WSKP_NGRAM.concat(sequence.set(sequence.size() - 1,
                                WSKP_WORD))).getOnePlusCount();
            }

            if (!lowestOrder) {
                NGram history = sequence.range(0, sequence.size() - 1);
                long historyCount = cache.getAbsolute(history);

                Discount discount = cache.getDiscount(model,
                        history.getPattern());
                double d = discount.getForCount(historyCount);

                numerator = Math.max(numerator - d, 0.0);
            }

            return numerator / denominator;
        }
    }

    protected String model;

    protected Path absoluteDir;
    protected Path alphaDir;
    protected Status status;
    protected Cache cache;

    public AlphaCalculator(Config config) {
        super(config);
        model = MODEL_MODKNESERNEY;
    }

    public void run(int order,
                    GlmtkPaths paths,
                    Status status) throws Exception {
        OUTPUT.setPhase(Phase.CALCULATING_ALPHAS);

        absoluteDir = paths.getAbsoluteDir();
        alphaDir = paths.getModKneserNeyAlphaDir();

        this.status = status;

        Set<Pattern> alphaPatterns = calcAlphaPatterns(order);
        alphaPatterns.removeAll(status.getAlphas(model));

        cache = getRequiredCache(alphaPatterns).build(paths);

        Files.createDirectories(alphaDir);

        work(alphaPatterns);
    }

    protected Set<Pattern> calcAlphaPatterns(int order) {
        Set<Pattern> result = new HashSet<>();
        Pattern pattern = Patterns.get();
        for (int i = 0; i != order; ++i) {
            pattern = pattern.concat(CNT);
            result.add(pattern);
        }
        return result;
    }

    protected CacheBuilder getRequiredCache(Set<Pattern> alphaPatterns) {
        Set<Pattern> countsPatterns = new HashSet<>();
        for (Pattern pattern : alphaPatterns) {
            // pattern to get highest order numerator
            countsPatterns.add(pattern);
            // pattern to get highest order denominator
            countsPatterns.add(pattern.set(pattern.size() - 1, SKP));
            // pattern to get lower orders numerator
            countsPatterns.add(WSKP_PATTERN.concat(pattern));
            // pattern to get lower orders denominator
            countsPatterns.add(WSKP_PATTERN.concat(pattern.set(
                    pattern.size() - 1, WSKP)));
        }

        return new CacheBuilder().withCounts(countsPatterns).withDiscounts(
                model);
    }

    @Override
    protected Collection<? extends Worker> createWorkers() {
        List<Worker> workers = new ArrayList<>();
        for (int i = 0; i != config.getNumberOfThreads(); ++i)
            workers.add(new Worker());
        return workers;
    }
}
