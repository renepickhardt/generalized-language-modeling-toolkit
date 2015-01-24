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

package de.glmtk.querying.estimator.fast;

import static de.glmtk.common.NGram.WSKP_NGRAM;
import de.glmtk.common.NGram;
import de.glmtk.counts.Counts;
import de.glmtk.counts.Discount;

public class FastModKneserNeyEstimator extends FastModKneserNeyAbsEstimator {
    @Override
    protected double calcProbability(NGram sequence,
                                     NGram history,
                                     int recDepth) {
        double denominator = cache.getAbsolute(getFullHistory(sequence, history));
        if (denominator == 0.0)
            return (double) cache.getAbsolute(sequence) / cache.getNumWords();

        double numerator = cache.getAbsolute(getFullSequence(sequence, history));
        if (history.isEmptyOrOnlySkips())
            return numerator / denominator;

        Discount d = getDiscounts(history.getPattern(), recDepth);
        double discount = d.getForCount(cache.getAbsolute(history));

        Counts c = cache.getContinuation(getFullHistory(sequence, history).convertSkpToWskp());
        double gamma = (d.getOne() * c.getOneCount() + d.getTwo()
                * c.getTwoCount() + d.getThree() * c.getThreePlusCount())
                / denominator;

        NGram backoffHistory = history.backoffUntilSeen(backoffMode, cache);
        double alpha = Math.max(numerator - discount, 0.0) / denominator;
        double beta = probabilityLower(sequence, backoffHistory, recDepth);

        return alpha + gamma * beta;
    }

    public final double probabilityLower(NGram sequence,
                                         NGram history,
                                         int recDepth) {
        logTrace(recDepth, "%s#probabilityLower(%s,%s)",
                getClass().getSimpleName(), sequence, history);
        ++recDepth;

        double result = calcProbabilityLower(sequence, history, recDepth);
        logTrace(recDepth, "result = %f", result);

        return result;
    }

    protected double calcProbabilityLower(NGram sequence,
                                          NGram history,
                                          int recDepth) {
        double denominator = cache.getContinuation(
                WSKP_NGRAM.concat(getFullHistory(sequence, history).convertSkpToWskp())).getOnePlusCount();
        if (denominator == 0.0)
            return (double) cache.getAbsolute(sequence) / cache.getNumWords();

        double numerator = cache.getContinuation(
                WSKP_NGRAM.concat(getFullSequence(sequence, history).convertSkpToWskp())).getOnePlusCount();
        if (history.isEmptyOrOnlySkips())
            return numerator / denominator;

        Discount d = getDiscounts(history.getPattern(), recDepth);
        double discount = d.getForCount(cache.getAbsolute(history));

        Counts c = cache.getContinuation(getFullHistory(sequence, history).convertSkpToWskp());
        double gamma = (d.getOne() * c.getOneCount() + d.getTwo()
                * c.getTwoCount() + d.getThree() * c.getThreePlusCount())
                / denominator;

        NGram backoffHistory = history.backoffUntilSeen(backoffMode, cache);
        double alpha = Math.max(numerator - discount, 0.0) / denominator;
        double beta = probabilityLower(sequence, backoffHistory, recDepth);

        return alpha + gamma * beta;
    }
}
