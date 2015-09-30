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

package de.glmtk.querying.estimator.fraction;

import de.glmtk.common.NGram;


public class FalseMaximumLikelihoodEstimator extends FractionEstimator {
    @Override
    public boolean isDefined(NGram sequence,
                             NGram history,
                             int recDepth) {
        return history.seen(cache);
    }

    @Override
    protected double calcNumerator(NGram sequence,
                                   NGram history,
                                   int recDepth) {
        NGram fullSequence = getFullSequence(sequence, history);
        long fullSequenceCount = cache.getCount(fullSequence);
        logTrace(recDepth, "fullSequence = %s (%d)", fullSequence,
            fullSequenceCount);
        return fullSequenceCount;
    }

    @Override
    protected double calcDenominator(NGram sequence,
                                     NGram history,
                                     int recDepth) {
        long historyCount;
        if (history.isEmpty()) {
            historyCount = cache.getNumWords();
        } else {
            historyCount = cache.getCount(history);
        }
        logTrace(recDepth, "fullHistory = %s (%d)", history, historyCount);
        return historyCount;
    }
}
