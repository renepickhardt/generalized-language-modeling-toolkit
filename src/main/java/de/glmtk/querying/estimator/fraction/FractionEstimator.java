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
import de.glmtk.exceptions.SwitchCaseNotImplementedException;
import de.glmtk.querying.estimator.Estimator;

public abstract class FractionEstimator extends Estimator {
    @Override
    protected double calcProbability(NGram sequence,
                                     NGram history,
                                     int recDepth) {
        if (history.seen(countCache)) {
            double numeratorVal = numerator(sequence, history, recDepth);
            double denominatorVal = denominator(sequence, history, recDepth);
            if (denominatorVal != 0)
                return numeratorVal / denominatorVal;
        }

        // Fraction estimator probability is undefined.
        switch (probMode) {
            case COND:
                logTrace(recDepth, "Probability undefined, returning 0 (COND):");
                return 0;
            case MARG:
                logTrace(recDepth,
                        "Probability undefined, returning substitute (MARG):");
                return SUBSTITUTE_ESTIMATOR.probability(sequence, history,
                        recDepth);
            default:
                throw new SwitchCaseNotImplementedException();
        }
    }

    public final double numerator(NGram sequence,
                                  NGram history,
                                  int recDepth) {
        logTrace(recDepth, "numerator(%s,%s)", sequence, history);
        ++recDepth;

        double result = calcNumerator(sequence, history, recDepth);
        logTrace(recDepth, "result = %f", result);
        return result;
    }

    protected abstract double calcNumerator(NGram sequence,
                                            NGram history,
                                            int recDepth);

    public final double denominator(NGram sequence,
                                    NGram history,
                                    int recDepth) {
        logTrace(recDepth, "denominator(%s,%s)", sequence, history);
        ++recDepth;

        double result = calcDenominator(sequence, history, recDepth);
        logTrace(recDepth, "result = %f", result);
        return result;
    }

    protected abstract double calcDenominator(NGram sequence,
                                              NGram history,
                                              int recDepth);
}
