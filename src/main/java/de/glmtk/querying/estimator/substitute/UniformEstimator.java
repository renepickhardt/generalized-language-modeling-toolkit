package de.glmtk.querying.estimator.substitute;

import de.glmtk.utils.NGram;

public class UniformEstimator extends SubstituteEstimator {

    @Override
    protected double
    calcProbability(NGram sequence, NGram history, int recDepth) {
        long vocabSize = countCache.getVocabSize();
        logDebug(recDepth, "vocabSize = {}", vocabSize);
        return 1.0 / vocabSize;
    }
}
