package de.glmtk.smoothing.legacy3.estimating;

import de.glmtk.smoothing.NGram;

public class UniformEstimator extends Estimator {

    @Override
    protected double
        calcProbability(NGram sequence, NGram history, int recDepth) {
        return 1.0 / corpus.getVocabSize();
    }

}
