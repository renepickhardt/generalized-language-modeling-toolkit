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

package de.glmtk.querying.estimator.discount;

import de.glmtk.cache.Cache;
import de.glmtk.common.NGram;
import de.glmtk.common.ProbMode;
import de.glmtk.querying.estimator.fraction.FractionEstimator;

public abstract class DiscountEstimator extends FractionEstimator {
    private FractionEstimator fractionEstimator;

    public DiscountEstimator(FractionEstimator fractionEstimator) {
        this.fractionEstimator = fractionEstimator;
    }

    public FractionEstimator getFractionEstimator() {
        return fractionEstimator;
    }

    @Override
    public void setCache(Cache cache) {
        super.setCache(cache);
        fractionEstimator.setCache(cache);
    }

    @Override
    public void setProbMode(ProbMode probMode) {
        super.setProbMode(probMode);
        fractionEstimator.setProbMode(probMode);
    }

    @Override
    public boolean isDefined(NGram sequence,
                             NGram history,
                             int recDepth) {
        return fractionEstimator.isDefined(sequence, history, recDepth);
    }

    public final double discount(NGram sequence,
                                 NGram history,
                                 int recDepth) {
        logTrace(recDepth, "discount(%s,%s)", sequence, history);
        ++recDepth;

        double result = calcDiscount(sequence, history, recDepth);
        logTrace(recDepth, "result = %f", result);
        return result;
    }

    protected abstract double calcDiscount(NGram sequence,
                                           NGram history,
                                           int recDepth);

    @Override
    protected double calcNumerator(NGram sequence,
                                   NGram history,
                                   int recDepth) {
        double numeratorVal = fractionEstimator.numerator(sequence, history,
                recDepth);
        double discountVal = discount(sequence, history, recDepth);

        return Math.max(numeratorVal - discountVal, 0);
    }

    @Override
    protected double calcDenominator(NGram sequence,
                                     NGram history,
                                     int recDepth) {
        return fractionEstimator.denominator(sequence, history, recDepth);
    }
}
