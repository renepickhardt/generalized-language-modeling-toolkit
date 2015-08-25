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

package de.glmtk.querying.estimator.interpol;

import static de.glmtk.common.NGram.SKP_NGRAM;
import static de.glmtk.common.PatternElem.WSKP_WORD;
import static java.lang.String.format;

import de.glmtk.cache.Cache;
import de.glmtk.common.BackoffMode;
import de.glmtk.common.NGram;
import de.glmtk.common.ProbMode;
import de.glmtk.querying.estimator.AbstractEstimator;
import de.glmtk.querying.estimator.Estimator;
import de.glmtk.querying.estimator.discount.DiscountEstimator;
import de.glmtk.querying.estimator.discount.ModKneserNeyDiscountEstimator;
import de.glmtk.querying.estimator.fraction.ContinuationMaximumLikelihoodEstimator;
import de.glmtk.querying.estimator.fraction.MaximumLikelihoodEstimator;

public class InterpolEstimator extends AbstractEstimator {
    protected DiscountEstimator alpha;
    protected Estimator beta;
    protected BackoffMode backoffMode;

    public InterpolEstimator(DiscountEstimator alpha,
                             Estimator beta) {
        this(alpha, beta, BackoffMode.DEL);
    }

    public InterpolEstimator(DiscountEstimator alpha) {
        this(alpha, BackoffMode.DEL);
    }

    public InterpolEstimator(DiscountEstimator alpha,
                             BackoffMode backoffMode) {
        this.alpha = alpha;
        beta = this;
        setBackoffMode(backoffMode);
    }

    public InterpolEstimator(DiscountEstimator alpha,
                             Estimator beta,
                             BackoffMode backoffMode) {
        this.alpha = alpha;
        this.beta = beta;
        setBackoffMode(backoffMode);
    }

    @Override
    public void setCache(Cache cache) {
        super.setCache(cache);
        alpha.setCache(cache);
        if (beta != this)
            beta.setCache(cache);
    }

    @Override
    public void setProbMode(ProbMode probMode) {
        super.setProbMode(probMode);
        alpha.setProbMode(probMode);
        if (beta != this)
            beta.setProbMode(probMode);
    }

    public void setBackoffMode(BackoffMode backoffMode) {
        if (backoffMode != BackoffMode.DEL && backoffMode != BackoffMode.SKP)
            throw new IllegalArgumentException(
                    "Illegal BackoffMode for this class.");
        this.backoffMode = backoffMode;
    }

    @Override
    protected double calcProbability(NGram sequence,
                                     NGram history,
                                     int recDepth) {
        if (history.isEmptyOrOnlySkips()) {
            logTrace(recDepth,
                    "history empty, returning fraction estimator probability");
            return alpha.getFractionEstimator().probability(sequence, history,
                    recDepth);
        } else if (!alpha.isDefined(sequence, history, recDepth)) {
            logTrace(recDepth, "Alpha undefined, backing off.");
            return probability(sequence, history.backoff(backoffMode),
                    recDepth);
        }

        NGram backoffHistory = history.backoffUntilSeen(backoffMode, cache);

        double alphaVal = alpha.probability(sequence, history, recDepth);
        double betaVal = beta.probability(sequence, backoffHistory, recDepth);
        double gammaVal = gamma(sequence, history, recDepth);

        logTrace(recDepth, "alpha = %e", alphaVal);
        logTrace(recDepth, "beta  = %e", betaVal);
        logTrace(recDepth, "gamma = %e", gammaVal);

        return alphaVal + gammaVal * betaVal;
    }

    public final double gamma(NGram sequence,
                              NGram history,
                              int recDepth) {
        logTrace(recDepth, "gamma(%s,%s)", sequence, history);
        ++recDepth;

        double result = calcGamma(sequence, history, recDepth);
        logTrace(recDepth, "result = %f", result);
        return result;
    }

    protected double calcGamma(NGram sequence,
                               NGram history,
                               int recDepth) {
        double denominator = alpha.denominator(sequence, history, recDepth);

        if (denominator == 0) {
            logTrace(recDepth, "denominator = 0, setting gamma = 0:");
            return 0;
        }

        if (alpha instanceof ModKneserNeyDiscountEstimator)
            if (alpha.getFractionEstimator() instanceof MaximumLikelihoodEstimator) {
                double gammaHigh = cache.getGammaHigh(history);
                logTrace(recDepth, "gammaHigh = %e", gammaHigh);
                return gammaHigh / denominator;
            } else
                if (alpha.getFractionEstimator() instanceof ContinuationMaximumLikelihoodEstimator) {
                double gammaLow = cache.getGammaLow(history);
                logTrace(recDepth, "gammaLow = %e", gammaLow);
                return gammaLow / denominator;
            } else
                throw new IllegalStateException(format(
                        "Fraction Estimator '%s' not implented in Interpolation Estimator.",
                        alpha.getFractionEstimator().getClass()));

        NGram gammaHistory;
        if (alpha.getFractionEstimator() instanceof MaximumLikelihoodEstimator)
            gammaHistory = history.concat(WSKP_WORD);
        else if (alpha.getFractionEstimator() instanceof ContinuationMaximumLikelihoodEstimator)
            gammaHistory = SKP_NGRAM.concat(history.concat(WSKP_WORD));
        else
            throw new IllegalStateException(format(
                    "Fraction Estimator '%s' not implented in Interpolation Estimator.",
                    alpha.getFractionEstimator().getClass()));

        logTrace(recDepth, "gammaHistory = %s", gammaHistory);

        double discount = alpha.discount(sequence, history, recDepth);
        double n_1p = cache.getCount(gammaHistory);

        logTrace(recDepth, "denominator = %f", denominator);
        logTrace(recDepth, "discount = %f", discount);
        logTrace(recDepth, "n_1p = %f", n_1p);

        return discount * n_1p / denominator;
    }
}
