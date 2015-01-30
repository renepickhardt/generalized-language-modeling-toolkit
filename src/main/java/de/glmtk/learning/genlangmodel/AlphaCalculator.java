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

package de.glmtk.learning.genlangmodel;

import static de.glmtk.Constants.MODEL_GENLANGMODEL;
import static de.glmtk.common.Pattern.WSKP_PATTERN;
import static de.glmtk.common.PatternElem.CNT;
import static de.glmtk.common.PatternElem.SKP;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.glmtk.Constants;
import de.glmtk.cache.CacheBuilder;
import de.glmtk.common.Config;
import de.glmtk.common.NGram;
import de.glmtk.common.Pattern;
import de.glmtk.common.Patterns;
import de.glmtk.counts.AlphaCounts;
import de.glmtk.files.AlphaCountsWriter;
import de.glmtk.files.SequenceReader;
import de.glmtk.util.StringUtils;

public class AlphaCalculator extends de.glmtk.learning.modkneserney.AlphaCalculator {
    private class Worker extends de.glmtk.learning.modkneserney.AlphaCalculator.Worker {
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

                    writer.append(sequenceString, alphas);
                }
            }

            status.addAlpha(model, pattern);
        }
    }

    public AlphaCalculator(Config config) {
        super(config);
        model = MODEL_GENLANGMODEL;
    }

    //    @Override
    //    public void run(int order,
    //                    GlmtkPaths paths,
    //                    Status status) {
    //        Set<Pattern> alphaPatterns = calcAlphaPatterns(order);
    //        System.out.println(alphaPatterns);
    //
    //        CacheBuilder requiredCache = getRequiredCache(alphaPatterns);
    //        System.out.println(requiredCache);
    //        System.out.println(new TreeSet<>(requiredCache.getCountsPatterns()));
    //    }

    @Override
    protected Set<Pattern> calcAlphaPatterns(int order) {
        Set<Pattern> result = new HashSet<>();
        for (int i = 1; i != order + 1; ++i)
            result.addAll(Patterns.getPermutations(i, CNT, SKP));
        return result;
    }

    @Override
    protected CacheBuilder getRequiredCache(Set<Pattern> alphaPatterns) {
        Set<Pattern> countsPatterns = new HashSet<>();
        for (Pattern pattern : alphaPatterns) {
            countsPatterns.add(pattern);
            if (pattern.contains(SKP))
                countsPatterns.add(WSKP_PATTERN.concat(pattern.convertSkpToWskp()));
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
