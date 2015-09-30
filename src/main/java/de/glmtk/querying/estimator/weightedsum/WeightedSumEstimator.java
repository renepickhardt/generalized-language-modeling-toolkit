package de.glmtk.querying.estimator.weightedsum;

import de.glmtk.common.NGram;
import de.glmtk.querying.estimator.Estimator;


public interface WeightedSumEstimator extends Estimator {
    public double probability(NGram sequence,
                              WeightedSumFunction weightedSumFunction);

    public WeightedSumFunction calcWeightedSumFunction(NGram history);
}
