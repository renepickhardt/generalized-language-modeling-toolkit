package de.glmtk.smoothing.estimating;

import de.glmtk.smoothing.NGram;

/**
 * {@code P_AbsUnigram(s | h) = c(s_1) / N}
 */
public class AbsoluteUnigramEstimator extends Estimator {

    @Override
    protected double
        calcPropability(NGram sequence, NGram history, int recDepth) {
        return (double) corpus.getAbsolute(sequence.firstWord())
                / corpus.getNumWords();
    }

}
