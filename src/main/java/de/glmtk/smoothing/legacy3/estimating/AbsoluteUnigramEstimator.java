package de.glmtk.smoothing.legacy3.estimating;

import de.glmtk.smoothing.NGram;

public class AbsoluteUnigramEstimator extends Estimator {

    @Override
    protected double
        calcProbability(NGram sequence, NGram history, int recDepth) {
        return (double) corpus.getAbsolute(sequence.firstWord())
                / corpus.getNumWords();
    }

}
