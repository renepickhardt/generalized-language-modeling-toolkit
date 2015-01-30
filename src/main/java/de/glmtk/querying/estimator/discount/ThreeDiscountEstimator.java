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

package de.glmtk.querying.estimator.discount;

import de.glmtk.common.NGram;
import de.glmtk.common.Pattern;
import de.glmtk.counts.Discounts;
import de.glmtk.querying.estimator.fraction.FractionEstimator;

public abstract class ThreeDiscountEstimator extends DiscountEstimator {
    public ThreeDiscountEstimator(FractionEstimator fractionEstimator) {
        super(fractionEstimator);
    }

    @Override
    protected double calcDiscount(NGram sequence,
                                  NGram history,
                                  int recDepth) {
        NGram fullSequence = getFullSequence(sequence, history);
        Discounts discounts = getDiscounts(fullSequence.getPattern());
        switch ((int) cache.getAbsolute(fullSequence)) {
            case 0:
                return 0;
            case 1:
                return discounts.getOne();
            case 2:
                return discounts.getTwo();
            default:
                return discounts.getThree();
        }
    }

    public abstract Discounts getDiscounts(Pattern pattern);
}