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

package de.glmtk.querying.estimator.learned;

import static de.glmtk.Constants.MODEL_MODKNESERNEY;
import static de.glmtk.common.NGram.WSKP_NGRAM;

import java.util.Arrays;

import de.glmtk.common.BackoffMode;
import de.glmtk.common.NGram;
import de.glmtk.counts.AlphaCounts;
import de.glmtk.counts.LambdaCounts;
import de.glmtk.querying.estimator.AbstractEstimator;

public class LearnedModKneserNeyEstimator extends AbstractEstimator {
    @Override
    protected double calcProbability(NGram sequence,
                                     NGram history,
                                     int recDepth) {
        cache.getAbsolute(new NGram(Arrays.asList("a", "a", "a", "a", "a")));
        cache.getAbsolute(new NGram(Arrays.asList("a", "a", "a", "a", "_")));
        cache.getContinuation(new NGram(Arrays.asList("%", "a")));
        cache.getContinuation(new NGram(Arrays.asList("%", "%")));
        boolean fullHistorySeen = getFullHistory(sequence, history).seen(cache);

        NGram alphaNGram = !fullHistorySeen ? sequence : getFullSequence(
                sequence, history);
        AlphaCounts alphas = cache.getAlpha(MODEL_MODKNESERNEY, alphaNGram);
        if (alphas == null) {
            logTrace(recDepth, "no alpha");
            logTrace(recDepth, "what to do?");
        }

        if (history.isEmptyOrOnlySkips() || !fullHistorySeen)
            return (double) cache.getAbsolute(getFullSequence(sequence, history))
                    / cache.getAbsolute(getFullHistory(sequence, history));

        LambdaCounts lambdas = cache.getLambda(MODEL_MODKNESERNEY, history);
        if (lambdas == null) {
            logTrace(recDepth, "no lambda");
            return 0.0;
        }

        double prob = 0.0;

        NGram h = history;
        int i = 0;
        int alphaShift = 0;
        boolean done;
        while (true) {
            double lambda = i == 0 ? 1.0 : lambdas.get(i - 1);
            double alpha = alphas == null ? 0.0 : alphas.get(i + alphaShift);

            if (i == 0)
                ++alphaShift;

            logTrace(recDepth, "lambda = %e  alpha = %e", lambda, alpha);

            prob += lambda * alpha;

            if (h.isEmptyOrOnlySkips())
                break;

            h = h.backoffUntilSeen(BackoffMode.DEL, cache);

            if (alphas == null) {
                fullHistorySeen = getFullHistory(sequence, h).seen(cache);

                if (h.isEmptyOrOnlySkips()) {
                    alphas = new AlphaCounts();
                    alphas.append(0.0);
                    alphas.append((double) cache.getContinuation(
                            WSKP_NGRAM.concat(getFullSequence(sequence, h).convertSkpToWskp())).getOnePlusCount()
                            / cache.getContinuation(
                                    WSKP_NGRAM.concat(getFullHistory(sequence,
                                            h).convertSkpToWskp())).getOnePlusCount());
                    logTrace(recDepth, "created alpha by hand");
                } else {
                    alphaNGram = !fullHistorySeen ? sequence : getFullSequence(
                            sequence, h);
                    alphas = cache.getAlpha(MODEL_MODKNESERNEY, alphaNGram);
                    logTrace(recDepth, "loaded alpha %s: %s", alphaNGram,
                            alphas);
                }
                --alphaShift;
            }

            ++i;
        }

        return prob;
    }
}
