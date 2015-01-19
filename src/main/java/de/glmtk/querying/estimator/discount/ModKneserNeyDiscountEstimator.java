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

import java.util.HashMap;
import java.util.Map;

import de.glmtk.common.CountCache;
import de.glmtk.common.NGram;
import de.glmtk.common.Pattern;
import de.glmtk.counts.Discount;
import de.glmtk.counts.NGramTimes;
import de.glmtk.querying.estimator.fraction.FractionEstimator;

public class ModKneserNeyDiscountEstimator extends DiscountEstimator {
    private Map<Pattern, Discount> discounts = null;

    public ModKneserNeyDiscountEstimator(FractionEstimator fractionEstimator) {
        super(fractionEstimator);
    }

    @Override
    public void setCountCache(CountCache countCache) {
        super.setCountCache(countCache);

        discounts = new HashMap<>();
    }

    @Override
    protected double calcDiscount(NGram sequence,
                                  NGram history,
                                  int recDepth) {
        Discount discounts = getDiscounts(history.getPattern());
        switch ((int) countCache.getAbsolute(history)) {
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

    public Discount getDiscounts(Pattern pattern) {
        Discount result = discounts.get(pattern);
        if (result != null)
            return result;

        NGramTimes n = countCache.getNGramTimes(pattern);
        double y = (double) n.getOneCount()
                / (n.getOneCount() + n.getTwoCount());
        result = new Discount(1.0f - 2.0f * y * n.getTwoCount()
                / n.getOneCount(), 2.0f - 3.0f * y * n.getThreeCount()
                / n.getTwoCount(), 3.0f - 4.0f * y * n.getFourCount()
                / n.getThreeCount());
        discounts.put(pattern, result);
        return result;
    }
}
