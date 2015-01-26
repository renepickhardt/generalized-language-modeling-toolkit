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

import java.util.ArrayList;
import java.util.List;

import de.glmtk.cache.CacheBuilder;
import de.glmtk.common.BackoffMode;
import de.glmtk.common.NGram;
import de.glmtk.common.Pattern;
import de.glmtk.common.PatternElem;
import de.glmtk.common.Patterns;
import de.glmtk.counts.AlphaCounts;
import de.glmtk.counts.LambdaCounts;
import de.glmtk.querying.estimator.AbstractEstimator;
import de.glmtk.util.StringUtils;

public class LearnedModKneserNeyEstimator extends AbstractEstimator {
    @Override
    protected double calcProbability(NGram sequence,
                                     NGram history,
                                     int recDepth) {
        AlphaCounts alphas = cache.getAlpha(MODEL_MODKNESERNEY,
                getFullSequence(sequence, history));

        if (history.isEmptyOrOnlySkips()
                || cache.getLambda(MODEL_MODKNESERNEY, history) == null) {
            if (alphas == null)
                // alphas may still be null after this
                alphas = cache.getAlpha(MODEL_MODKNESERNEY, sequence);
            if (alphas == null)
                return 0.0;
            return alphas.get(0);
        }

        LambdaCounts lambdas = cache.getLambda(MODEL_MODKNESERNEY, history);
        if (lambdas == null)
            return 0.0;

        double prob = alphas == null ? 0.0 : alphas.get(1);
        logTrace(recDepth, "alpha = %e", prob);

        NGram h = history;
        int alphaShift = 0;
        for (int i = 0; !h.isEmptyOrOnlySkips(); ++i) {
            h = h.backoffUntilSeenUsingAlphas(BackoffMode.DEL, cache,
                    MODEL_MODKNESERNEY);

            if (alphas == null) {
                // alphas may still be null after this
                alphas = cache.getAlpha(MODEL_MODKNESERNEY, getFullSequence(
                        sequence, h));
                ++alphaShift;
            }

            double lambda = lambdas.get(i);
            double alpha = (alphas == null ? 0.0 : alphas.get(i + 3
                    - alphaShift));
            logTrace(recDepth, "lambda = %e  alpha = %e", lambda, alpha);

            prob += lambda * alpha;
        }

        return prob;
    }

    @Override
    public CacheBuilder getRequiredCache(int modelSize) {
        CacheBuilder cacheBuilder = new CacheBuilder();

        List<Pattern> cntPatterns = new ArrayList<>(modelSize);
        for (int i = 0; i != modelSize; ++i)
            cntPatterns.add(Patterns.get(StringUtils.repeat(
                    PatternElem.CNT.toString(), i + 1)));

        cacheBuilder.withAlphas(MODEL_MODKNESERNEY, cntPatterns);
        cacheBuilder.withLambdas(MODEL_MODKNESERNEY, cntPatterns.subList(0,
                cntPatterns.size() - 1));

        return cacheBuilder;
    }
}
