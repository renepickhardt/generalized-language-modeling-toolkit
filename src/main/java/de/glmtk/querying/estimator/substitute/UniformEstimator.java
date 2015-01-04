package de.glmtk.querying.estimator.substitute;

import de.glmtk.common.NGram;

public class UniformEstimator extends SubstituteEstimator {
    @Override
    protected double calcProbability(NGram sequence,
                                     NGram history,
                                     int recDepth) {
        long vocabSize = countCache.getVocabSize();
        logTrace(recDepth, "vocabSize = %d", vocabSize);
        return 1.0 / vocabSize;
    }
}
