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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.PriorityQueue;

import de.glmtk.cache.Cache;
import de.glmtk.common.NGram;
import de.glmtk.querying.estimator.Estimator;
import de.glmtk.querying.estimator.weightedsum.WeightedSumEstimator;
import de.glmtk.querying.estimator.weightedsum.WeightedSumFunction;
import de.glmtk.util.CollectionUtils;
import de.glmtk.util.StringUtils;


public class TrivialArgmaxQueryExecutor implements ArgmaxQueryExecutor {
    // TODO: ArgmaxComputation with this class tries out all possible arguments.
    // A nice test case would be to check if those probabilities sum to one.

    private Estimator estimator;
    private WeightedSumEstimator weightedSumEstimator;
    @SuppressWarnings("unused")
    private Cache randomAccessCache;
    private Collection<String> vocab;

    public TrivialArgmaxQueryExecutor(Estimator estimator,
                                      Cache randomAccessCache,
                                      Collection<String> vocab) {
        this.estimator = estimator;
        if (estimator instanceof WeightedSumEstimator) {
            weightedSumEstimator = (WeightedSumEstimator) estimator;
        }
        this.randomAccessCache = randomAccessCache;
        this.vocab = vocab;
    }

    public TrivialArgmaxQueryExecutor(Estimator estimator,
                                      Cache randomAccessCache) {
        this(estimator, randomAccessCache, randomAccessCache.getWords());
    }

    @Override
    public List<ArgmaxResult> queryArgmax(String history,
                                          int numResults) {
        return queryArgmax(history, "", numResults);
    }

    @Override
    public List<ArgmaxResult> queryArgmax(String history,
                                          String prefix,
                                          int numResults) {
        if (numResults == 0) {
            return new ArrayList<>();
        }
        if (numResults < 0) {
            throw new IllegalArgumentException("numResults must be positive.");
        }

        NGram hist = new NGram(StringUtils.split(history, ' '));
        WeightedSumFunction weightedSumFunction = null;
        if (weightedSumEstimator != null) {
            weightedSumFunction =
                weightedSumEstimator.calcWeightedSumFunction(hist);
        }

        PriorityQueue<ArgmaxResult> queue =
            new PriorityQueue<>(numResults + 1, ArgmaxResult.COMPARATOR);
        for (String sequence : vocab) {
            if (!sequence.startsWith(prefix)) {
                continue;
            }

            NGram seq = new NGram(sequence);

            double prob;
            if (weightedSumEstimator != null) {
                prob =
                    weightedSumEstimator.probability(seq, weightedSumFunction);
            } else {
                prob = estimator.probability(seq, hist);
            }

            if (prob == 0.0) {
                continue;
            }

            queue.add(new ArgmaxResult(sequence, prob));
            if (queue.size() > numResults) {
                queue.poll();
            }
        }

        return CollectionUtils.drainQueueToList(queue);
    }
}
