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
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import de.glmtk.GlmtkPaths;
import de.glmtk.cache.Cache;
import de.glmtk.cache.CacheBuilder;
import de.glmtk.common.NGram;
import de.glmtk.common.Patterns;
import de.glmtk.querying.estimator.Estimator;
import de.glmtk.querying.estimator.iterative.IterativeGenLangModelEstimator;
import de.glmtk.querying.estimator.iterative.IterativeGenLangModelEstimator.GlmNode;
import de.glmtk.querying.estimator.iterative.IterativeModKneserNeyEstimator;
import de.glmtk.util.BinomDiamond;
import de.glmtk.util.CollectionUtils;
import de.glmtk.util.StringUtils;

public class ArgmaxQueryExecutor {
    public static class ArgmaxResult {
        private String sequence;
        private double probability;

        public ArgmaxResult(String sequence,
                            double probability) {
            this.sequence = sequence;
            this.probability = probability;
        }

        public String getSequence() {
            return sequence;
        }

        public double getProbability() {
            return probability;
        }

        @Override
        public String toString() {
            return String.format("%s\t%e", sequence, probability);
        }
    }

    private static class ArgmaxResultComparator implements Comparator<ArgmaxResult> {
        @Override
        public int compare(ArgmaxResult lhs,
                           ArgmaxResult rhs) {
            return Double.compare(lhs.probability, rhs.probability);
        }
    }

    private static final Comparator<ArgmaxResult> ARGMAX_RESULT_COMPARATOR = new ArgmaxResultComparator();

    private IterativeModKneserNeyEstimator iterativeModKneserNeyEstimator;
    private IterativeGenLangModelEstimator iterativeGenLangModelEstimator;
    private Estimator estimator;
    private Collection<String> vocab;

    public ArgmaxQueryExecutor(IterativeModKneserNeyEstimator estimator,
                               GlmtkPaths paths) throws IOException {
        iterativeModKneserNeyEstimator = estimator;
        iterativeGenLangModelEstimator = null;
        vocab = loadVocabFromCounts(paths);
    }

    public ArgmaxQueryExecutor(IterativeGenLangModelEstimator estimator,
                               GlmtkPaths paths) throws IOException {
        iterativeModKneserNeyEstimator = null;
        iterativeGenLangModelEstimator = estimator;
        vocab = loadVocabFromCounts(paths);
    }

    public ArgmaxQueryExecutor(Estimator estimator,
                               GlmtkPaths paths) throws IOException {
        iterativeModKneserNeyEstimator = null;
        iterativeGenLangModelEstimator = null;
        this.estimator = estimator;
        vocab = loadVocabFromCounts(paths);
    }

    public ArgmaxQueryExecutor(IterativeModKneserNeyEstimator estimator,
                               Collection<String> vocab) {
        iterativeModKneserNeyEstimator = estimator;
        iterativeGenLangModelEstimator = null;
        this.vocab = vocab;
    }

    public ArgmaxQueryExecutor(IterativeGenLangModelEstimator estimator,
                               Collection<String> vocab) {
        iterativeModKneserNeyEstimator = null;
        iterativeGenLangModelEstimator = estimator;
        this.vocab = vocab;
    }

    private Collection<String> loadVocabFromCounts(GlmtkPaths paths) throws IOException {
        Cache cache = new CacheBuilder().withCounts(Patterns.getMany("1")).build(
                paths);
        return cache.getWords();
    }

    public List<ArgmaxResult> queryArgmax(String history) {
        NGram hist = new NGram(StringUtils.split(history, ' '));
        Object historyData = null;
        if (iterativeModKneserNeyEstimator != null)
            historyData = iterativeModKneserNeyEstimator.calcLambdas(hist);
        else if (iterativeGenLangModelEstimator != null)
            historyData = iterativeGenLangModelEstimator.buildGlmDiamond(hist);
        else if (estimator == null)
            throw new IllegalStateException("No estimator set.");

        PriorityQueue<ArgmaxResult> queue = new PriorityQueue<>(6,
                ARGMAX_RESULT_COMPARATOR);
        for (String sequence : vocab) {
            NGram seq = new NGram(sequence);

            double prob;
            if (iterativeModKneserNeyEstimator != null) {
                @SuppressWarnings("unchecked")
                List<Double> lambdas = (List<Double>) historyData;
                prob = iterativeModKneserNeyEstimator.probability(seq, hist,
                        lambdas);
            } else if (iterativeGenLangModelEstimator != null) {
                @SuppressWarnings("unchecked")
                BinomDiamond<GlmNode> glmDiamond = (BinomDiamond<GlmNode>) historyData;
                prob = iterativeGenLangModelEstimator.probability(seq, hist,
                        glmDiamond);
            } else if (estimator == null)
                throw new IllegalStateException("No estimator set.");
            else
                prob = estimator.probability(seq, hist);

            queue.add(new ArgmaxResult(sequence, prob));
            if (queue.size() > 5)
                queue.poll();
        }

        return CollectionUtils.drainQueueToList(queue);
    }
}
