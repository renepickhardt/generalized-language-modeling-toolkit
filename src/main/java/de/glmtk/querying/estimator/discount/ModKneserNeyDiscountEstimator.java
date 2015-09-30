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
import de.glmtk.querying.estimator.fraction.ContinuationMaximumLikelihoodEstimator;
import de.glmtk.querying.estimator.fraction.FractionEstimator;
import de.glmtk.querying.estimator.fraction.MaximumLikelihoodEstimator;


public class ModKneserNeyDiscountEstimator extends DiscountEstimator {
    public ModKneserNeyDiscountEstimator(FractionEstimator fractionEstimator) {
        super(fractionEstimator);
    }

    @Override
    protected double calcDiscount(NGram sequence,
                                  NGram history,
                                  int recDepth) {
        NGram fullSequence;
        if (getFractionEstimator() instanceof MaximumLikelihoodEstimator) {
            fullSequence = getFullSequence(sequence, history);
        } else
            if (getFractionEstimator() instanceof ContinuationMaximumLikelihoodEstimator) {
            fullSequence = NGram.WSKP_NGRAM
                .concat(getFullSequence(sequence, history).convertSkpToWskp());
        } else {
            throw new IllegalStateException();
        }
        return cache.getDiscount(fullSequence);
    }
}
