package de.glmtk.querying.estimator.fast;

import de.glmtk.common.BackoffMode;
import de.glmtk.common.NGram;
import de.glmtk.querying.estimator.Estimator;

public class FastModifiedKneserNeyEstimator extends Estimator {
    protected BackoffMode backoffMode;

    public FastModifiedKneserNeyEstimator() {
        setBackoffMode(BackoffMode.DEL);
    }

    public void setBackoffMode(BackoffMode backoffMode) {
        if (backoffMode != BackoffMode.DEL && backoffMode != BackoffMode.SKP)
            throw new IllegalArgumentException(
                    "Illegal BackoffMode for this class.");
        this.backoffMode = backoffMode;
    }

    @Override
    protected double calcProbability(NGram sequence,
                                     NGram history,
                                     int recDepth) {
        return 0;
    }

}
