package de.glmtk.smoothing.estimator.substitute;

import de.glmtk.smoothing.NGram;

public class AbsoluteUnigramEstimator extends SubstituteEstimator {

    @Override
    protected double
        calcProbability(NGram sequence, NGram history, int recDepth) {
        return (double) countCache.getAbsolute(sequence.get(0))
                / countCache.getNumWords();
    }
}
