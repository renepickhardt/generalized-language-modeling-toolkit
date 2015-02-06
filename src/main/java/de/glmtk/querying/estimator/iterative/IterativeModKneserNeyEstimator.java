/*
 * Generalized Language Modeling Toolkit (GLMTK)
 * 
 * Copyright (C) 2015 Lukas Schmelzeisen, Rene Pickhardt
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

package de.glmtk.querying.estimator.iterative;

import static de.glmtk.common.NGram.SKP_NGRAM;
import static de.glmtk.common.NGram.WSKP_NGRAM;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.glmtk.cache.Cache;
import de.glmtk.common.BackoffMode;
import de.glmtk.common.NGram;
import de.glmtk.common.Pattern;
import de.glmtk.common.PatternElem;
import de.glmtk.counts.Counts;
import de.glmtk.counts.Discounts;
import de.glmtk.counts.NGramTimes;
import de.glmtk.querying.estimator.AbstractEstimator;

public class IterativeModKneserNeyEstimator extends AbstractEstimator {
    protected BackoffMode backoffMode;
    private Map<Pattern, Discounts> discounts;

    public IterativeModKneserNeyEstimator() {
        setBackoffMode(BackoffMode.DEL);
        discounts = new HashMap<>();
    }

    public void setBackoffMode(BackoffMode backoffMode) {
        if (backoffMode != BackoffMode.DEL && backoffMode != BackoffMode.SKP)
            throw new IllegalArgumentException(
                    "Illegal BackoffMode for this class.");
        this.backoffMode = backoffMode;
    }

    @Override
    public void setCache(Cache cache) {
        super.setCache(cache);
        discounts = new HashMap<>();
    }

    public double probability(NGram sequence,
                              NGram history,
                              List<Double> lambdas) {
        return calcProbability(sequence, history, 1, lambdas);
    }

    @Override
    protected double calcProbability(NGram sequence,
                                     NGram history,
                                     int recDepth) {
        List<Double> lambdas = calcLambdas(history);
        return calcProbability(sequence, history, recDepth, lambdas);
    }

    protected double calcProbability(NGram sequence,
                                     NGram history,
                                     @SuppressWarnings("unused") int recDepth,
                                     List<Double> lambdas) {
        NGram hist = history;
        double prob = 0.0;
        boolean absolute = true;
        boolean last = false;
        for (int i = 0; !last; ++i) {
            if (i != 0)
                hist = hist.backoff(backoffMode);
            last = hist.isEmptyOrOnlySkips();

            double lambda = lambdas.get(i);
            if (lambda == 0.0)
                continue;

            double alpha = calcAlpha(getFullSequence(sequence, hist), absolute,
                    !last);
            absolute = false;

            prob += alpha * lambda;
        }

        return prob;
    }

    /**
     * @param absolute
     *            If {@code true} alpha absolute count, if {@code false} alpha
     *            continuation count.
     * @param discount
     *            If {@code true} discount alpha if {@code false} not.
     */
    protected double calcAlpha(NGram sequence,
                               boolean absolute,
                               boolean discount) {
        long absSequenceCount = cache.getAbsolute(sequence);
        double alpha;
        if (absolute)
            alpha = absSequenceCount;
        else
            alpha = cache.getContinuation(
                    WSKP_NGRAM.concat(sequence.convertSkpToWskp())).getOnePlusCount();

        if (discount) {
            Discounts disc = calcDiscounts(sequence.getPattern());
            double d = disc.getForCount(absSequenceCount);

            alpha = Math.max(alpha - d, 0.0);
        }

        return alpha;
    }

    public List<Double> calcLambdas(NGram history) {
        List<Double> lambdas = new ArrayList<>(history.size());
        NGram fullHistory = history.concat(SKP_NGRAM);
        while (!fullHistory.seen(cache)) {
            lambdas.add(0.0);
            fullHistory = fullHistory.backoff(backoffMode);
        }

        NGram hist = fullHistory.remove(fullHistory.size() - 1);
        int order = fullHistory.getPattern().numElems(PatternElem.CNT);
        double lambda = 1.0;
        long denominator = cache.getAbsolute(hist.concat(SKP_NGRAM));
        lambdas.add(lambda / denominator);
        for (int i = 0; i != order; ++i) {
            lambda *= calcGamma(hist, denominator);
            hist = hist.backoff(backoffMode);
            denominator = cache.getContinuation(
                    WSKP_NGRAM.concat(hist.convertSkpToWskp()).concat(
                            WSKP_NGRAM)).getOnePlusCount();
            lambdas.add(lambda / denominator);
        }

        return lambdas;
    }

    protected double calcGamma(NGram history,
                               long denominator) {
        Discounts discount = calcDiscounts(history.getPattern().concat(
                PatternElem.CNT));
        Counts contCount = cache.getContinuation(history.concat(WSKP_NGRAM));

        return (discount.getOne() * contCount.getOneCount() + discount.getTwo()
                * contCount.getTwoCount() + discount.getThree()
                * contCount.getThreePlusCount())
                / denominator;
    }

    protected Discounts calcDiscounts(Pattern pattern) {
        Discounts result = discounts.get(pattern);
        if (result != null)
            return result;

        NGramTimes n = cache.getNGramTimes(pattern);
        double y = (double) n.getOneCount()
                / (n.getOneCount() + n.getTwoCount());
        result = new Discounts(1.0f - 2.0f * y * n.getTwoCount()
                / n.getOneCount(), 2.0f - 3.0f * y * n.getThreeCount()
                / n.getTwoCount(), 3.0f - 4.0f * y * n.getFourCount()
                / n.getThreeCount());

        discounts.put(pattern, result);
        return result;
    }
}
