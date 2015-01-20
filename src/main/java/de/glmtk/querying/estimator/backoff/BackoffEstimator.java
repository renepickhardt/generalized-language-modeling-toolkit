/*
 * Generalized Language Modeling Toolkit (GLMTK)
 *
 * Copyright (C) 2014-2015 Lukas Schmelzeisen, Rene Pickhardt
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

package de.glmtk.querying.estimator.backoff;

import java.util.HashMap;
import java.util.Map;

import de.glmtk.common.BackoffMode;
import de.glmtk.common.Cache;
import de.glmtk.common.NGram;
import de.glmtk.common.ProbMode;
import de.glmtk.exceptions.SwitchCaseNotImplementedException;
import de.glmtk.querying.estimator.AbstractEstimator;
import de.glmtk.querying.estimator.Estimator;

public class BackoffEstimator extends AbstractEstimator {
    private Map<NGram, Double> gammaCache;
    private Estimator alpha;
    private Estimator beta;

    public BackoffEstimator(Estimator alpha) {
        this.alpha = alpha;
        beta = this;
    }

    public BackoffEstimator(Estimator alpha,
                            Estimator beta) {
        this.alpha = alpha;
        this.beta = beta;
    }

    @Override
    public void setCache(Cache cache) {
        super.setCache(cache);
        alpha.setCache(cache);
        if (beta != this)
            beta.setCache(cache);

        gammaCache = new HashMap<>();
    }

    @Override
    public void setProbMode(ProbMode probMode) {
        super.setProbMode(probMode);
        alpha.setProbMode(probMode);
        if (beta != this)
            beta.setProbMode(probMode);

        gammaCache = new HashMap<>();
    }

    @Override
    protected double calcProbability(NGram sequence,
                                     NGram history,
                                     int recDepth) {
        if (history.isEmpty())
            switch (probMode) {
                case COND:
                    return 0;
                case MARG:
                    return SUBSTITUTE_ESTIMATOR.probability(sequence, history,
                            recDepth);
                default:
                    throw new SwitchCaseNotImplementedException();
            }
        else if (cache.getAbsolute(getFullSequence(sequence, history)) == 0) {
            NGram backoffHistory = history.backoff(BackoffMode.DEL); // TODO: fix backoff arg

            double betaVal = beta.probability(sequence, backoffHistory,
                    recDepth);
            double gammaVal = gamma(sequence, history, recDepth);
            logTrace(recDepth, "beta = %f", betaVal);
            logTrace(recDepth, "gamma = %f", gammaVal);
            return gammaVal * betaVal;
        } else {
            double alphaVal = alpha.probability(sequence, history, recDepth);
            logTrace(recDepth, "alpha = %f", alphaVal);
            return alphaVal;
        }
    }

    /**
     * Wrapper around {@link #calcGamma(NGram, NGram, int)} to add logging and
     * caching.
     */
    public double gamma(NGram sequence,
                        NGram history,
                        int recDepth) {
        logTrace(recDepth, "gamma(%s,%s)", sequence, history);
        ++recDepth;

        Double result = gammaCache.get(history);
        if (result != null) {
            logTrace(recDepth, "result = %f was cached.", result);
            return result;
        }

        result = calcGamma(sequence, history, recDepth);
        gammaCache.put(history, result);
        logTrace(recDepth, "result = %f", result);
        return result;
    }

    @SuppressWarnings("unused")
    public double calcGamma(NGram sequence,
                            NGram history,
                            int recDepth) {
        double sumAlpha = 0;
        double sumBeta = 0;

        NGram backoffHistory = history.backoff(BackoffMode.DEL); // TODO: fix backoff arg

        for (String word : cache.getWords()) {
            NGram s = history.concat(word);
            if (cache.getAbsolute(s) == 0)
                sumBeta += beta.probability(new NGram(word), backoffHistory,
                        recDepth);
            else
                sumAlpha += alpha.probability(new NGram(word), history,
                        recDepth);
        }

        logTrace(recDepth, "sumAlpha = %f", sumAlpha);
        logTrace(recDepth, "sumBeta = %f", sumBeta);

        if (sumBeta == 0)
            return 0.0;
        return (1.0 - sumAlpha) / sumBeta;
    }
}
