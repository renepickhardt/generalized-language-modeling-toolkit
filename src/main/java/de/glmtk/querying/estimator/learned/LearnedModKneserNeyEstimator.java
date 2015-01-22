package de.glmtk.querying.estimator.learned;

import de.glmtk.common.NGram;
import de.glmtk.querying.estimator.AbstractEstimator;

public class LearnedModKneserNeyEstimator extends AbstractEstimator {
    @Override
    protected double calcProbability(NGram sequence,
                                     NGram history,
                                     int recDepth) {
        return Double.NaN;
    }
}
