package de.glmtk.smoothing.estimating;

import de.glmtk.smoothing.NGram;

/**
 * {@code P_Uniform(s | h) = 1 / V}
 */
public class UniformEstimator extends Estimator {

    @Override
    protected double
        calcProbability(NGram sequence, NGram history, int recDepth) {
        return 1.0 / corpus.getVocabSize();
    }

}
