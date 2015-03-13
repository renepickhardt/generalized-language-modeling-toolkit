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

package de.glmtk.querying.argmax;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.PriorityQueue;

import de.glmtk.GlmtkPaths;
import de.glmtk.cache.Cache;
import de.glmtk.cache.CacheBuilder;
import de.glmtk.common.NGram;
import de.glmtk.common.Patterns;
import de.glmtk.querying.estimator.Estimator;
import de.glmtk.querying.estimator.weightedsum.WeightedSumEstimator;
import de.glmtk.querying.estimator.weightedsum.WeightedSumFunction;
import de.glmtk.util.CollectionUtils;
import de.glmtk.util.StringUtils;

public class TrivialArgmaxQueryExecutor implements ArgmaxQueryExecutor {
    private Estimator estimator;
    private WeightedSumEstimator weightedSumEstimator;
    private Collection<String> vocab;

    public TrivialArgmaxQueryExecutor(Estimator estimator,
                                      Collection<String> vocab) {
        this.estimator = estimator;
        weightedSumEstimator = null;
        if (estimator instanceof WeightedSumEstimator)
            weightedSumEstimator = (WeightedSumEstimator) estimator;
        this.vocab = vocab;
    }

    public TrivialArgmaxQueryExecutor(Estimator estimator,
                                      GlmtkPaths paths) throws IOException {
        this(estimator, loadVocabFromCounts(paths));
    }

    private static Collection<String> loadVocabFromCounts(GlmtkPaths paths) throws IOException {
        Cache cache = new CacheBuilder().withCounts(Patterns.getMany("1")).build(
                paths);
        return cache.getWords();
    }

    @Override
    public List<ArgmaxResult> queryArgmax(String history) {
        NGram hist = new NGram(StringUtils.split(history, ' '));
        WeightedSumFunction weightedSumFunction = null;
        if (weightedSumEstimator != null)
            weightedSumFunction = weightedSumEstimator.calcWeightedSumFunction(hist);

        PriorityQueue<ArgmaxResult> queue = new PriorityQueue<>(6,
                ArgmaxResult.COMPARATOR);
        for (String sequence : vocab) {
            NGram seq = new NGram(sequence);

            double prob;
            if (weightedSumEstimator != null)
                prob = weightedSumEstimator.probability(seq,
                        weightedSumFunction);
            else
                prob = estimator.probability(seq, hist);

            queue.add(new ArgmaxResult(sequence, prob));
            if (queue.size() > 5)
                queue.poll();
        }

        return CollectionUtils.drainQueueToList(queue);
    }
}
