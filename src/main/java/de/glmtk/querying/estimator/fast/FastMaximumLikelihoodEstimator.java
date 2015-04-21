package de.glmtk.querying.estimator.fast;

import de.glmtk.common.BackoffMode;
import de.glmtk.common.NGram;
import de.glmtk.querying.estimator.AbstractEstimator;

public class FastMaximumLikelihoodEstimator extends AbstractEstimator {

    @Override
    protected double calcProbability(NGram sequence,
                                     NGram history,
                                     int recDepth) {
        long denominator = cache.getAbsolute(getFullHistory(sequence, history));
        if (denominator == 0.0)
            return probability(sequence, history.backoff(BackoffMode.DEL),
                    recDepth);

        long numerator = cache.getAbsolute(getFullSequence(sequence, history));

        return (double) numerator / denominator;
    }

}
