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

package de.glmtk.querying.estimator.combination;

import de.glmtk.common.CountCache;
import de.glmtk.common.NGram;
import de.glmtk.common.ProbMode;
import de.glmtk.querying.estimator.AbstractEstimator;
import de.glmtk.querying.estimator.Estimator;

public class CombinationEstimator extends AbstractEstimator {
    private double lambda;
    private Estimator alpha;
    private Estimator beta;

    public CombinationEstimator(Estimator alpha,
                                Estimator beta,
                                double lambda) {
        this.alpha = alpha;
        this.beta = beta;
        this.lambda = lambda;
    }

    @Override
    public void setCountCache(CountCache countCache) {
        super.setCountCache(countCache);
        alpha.setCountCache(countCache);
        beta.setCountCache(countCache);
    }

    @Override
    public void setProbMode(ProbMode probMode) {
        super.setProbMode(probMode);
        alpha.setProbMode(probMode);
        beta.setProbMode(probMode);
    }

    @Override
    protected double calcProbability(NGram sequence,
                                     NGram history,
                                     int recDepth) {
        double alphaVal = alpha.probability(sequence, history, recDepth);
        double betaVal = beta.probability(sequence, history, recDepth);
        logTrace(recDepth, "alpha = %f", alphaVal);
        logTrace(recDepth, "beta = %f", betaVal);
        return lambda * alphaVal + (1.0 - lambda) * betaVal;
    }
}
