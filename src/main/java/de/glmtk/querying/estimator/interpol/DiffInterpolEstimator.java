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

import java.util.Set;

import de.glmtk.common.BackoffMode;
import de.glmtk.common.NGram;
import de.glmtk.querying.estimator.Estimator;
import de.glmtk.querying.estimator.discount.DiscountEstimator;

public class DiffInterpolEstimator extends InterpolEstimator {
    public DiffInterpolEstimator(DiscountEstimator alpha) {
        super(alpha, BackoffMode.SKP);
    }

    public DiffInterpolEstimator(DiscountEstimator alpha,
                                 Estimator beta) {
        super(alpha, beta, BackoffMode.SKP);
    }

    public DiffInterpolEstimator(DiscountEstimator alpha,
                                 Estimator beta,
                                 BackoffMode backoffMode) {
        super(alpha, beta, backoffMode);
    }

    public DiffInterpolEstimator(DiscountEstimator alpha,
                                 BackoffMode backoffMode) {
        super(alpha, backoffMode);
    }

    @Override
    public void setBackoffMode(BackoffMode backoffMode) {
        // Here all backoffModes are allowed, as opposed to
        // {@link InterpolEstimator#setBackoffMode(BackoffMode)}.
        this.backoffMode = backoffMode;
    }

    @Override
    protected double calcProbability(NGram sequence,
                                     NGram history,
                                     int recDepth) {
        if (history.isEmptyOrOnlySkips())
            //if (history.isEmpty()) {
            return super.calcProbability(sequence, history, recDepth);
        else if (!alpha.isDefined(sequence, history, recDepth)) {
            logTrace(recDepth, "Alpha undefined, averaging backoffs.");
            double result = 0;
            Set<NGram> diffHistories = history.getDifferentiatedNGrams(backoffMode);
            for (NGram diffHistory : diffHistories)
                result += probability(sequence, diffHistory, recDepth);
            result /= diffHistories.size();
            return result;
        }

        double alphaVal = alpha.probability(sequence, history, recDepth);
        double betaVal = 0;
        Set<NGram> diffHistories = history.getDifferentiatedNGrams(backoffMode);
        for (NGram diffHistory : diffHistories)
            betaVal += beta.probability(sequence, diffHistory, recDepth);
        betaVal /= diffHistories.size();
        double gammaVal = gamma(sequence, history, recDepth);

        return alphaVal + gammaVal * betaVal;
    }
}
