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

package de.glmtk.querying.estimator.weightedsum;

import static de.glmtk.common.NGram.SKP_NGRAM;
import static de.glmtk.common.NGram.WSKP_NGRAM;
import static de.glmtk.common.PatternElem.CNT;
import de.glmtk.common.BackoffMode;
import de.glmtk.common.NGram;
import de.glmtk.counts.Counts;
import de.glmtk.counts.Discounts;

public class WeightedSumModKneserNeyEstimator extends AbstractWeightedSumEstimator {
    protected BackoffMode backoffMode;

    public WeightedSumModKneserNeyEstimator() {
        super();
        setBackoffMode(BackoffMode.DEL);
    }

    public void setBackoffMode(BackoffMode backoffMode) {
        if (backoffMode != BackoffMode.DEL && backoffMode != BackoffMode.SKP)
            throw new IllegalArgumentException(
                    "Illegal BackoffMode for this class.");
        this.backoffMode = backoffMode;
    }

    @Override
    public WeightedSumFunction calcWeightedSumFunction(NGram history) {
        WeightedSumFunction weightedSumFunction = new WeightedSumFunction(
                history.size());

        NGram fullHistory = history.concat(SKP_NGRAM);
        while (!fullHistory.seen(cache))
            fullHistory = fullHistory.backoff(backoffMode);

        NGram hist = fullHistory.remove(fullHistory.size() - 1);
        int order = fullHistory.getPattern().numElems(CNT);
        double lambda = 1.0;
        long denominator = cache.getAbsolute(hist.concat(SKP_NGRAM));
        weightedSumFunction.add(lambda / denominator, hist);
        for (int i = 0; i != order; ++i) {
            lambda *= calcGamma(hist, denominator);
            hist = hist.backoff(backoffMode);

            denominator = cache.getContinuation(
                    WSKP_NGRAM.concat(hist).concat(WSKP_NGRAM)).getOnePlusCount();

            weightedSumFunction.add(lambda / denominator,
                    WSKP_NGRAM.concat(hist.convertSkpToWskp()));
        }

        return weightedSumFunction;
    }

    protected double calcGamma(NGram history,
                               long denominator) {
        Discounts discount = calcDiscounts(history.getPattern().concat(CNT));
        Counts contCount = cache.getContinuation(history.concat(WSKP_NGRAM));

        return (discount.getOne() * contCount.getOneCount() + discount.getTwo()
                * contCount.getTwoCount() + discount.getThree()
                * contCount.getThreePlusCount())
                / denominator;
    }
}
