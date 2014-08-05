package de.glmtk.smoothing.estimating;

import de.glmtk.smoothing.CalculatingMode;
import de.glmtk.smoothing.NGram;

/**
 * {@code P_AbsUnigram(s | h) = c(s_1) / N}
 */
public class AbsoluteUnigramEstimator extends Estimator {

    @Override
    protected double
        calcProbability(NGram sequence, NGram history, CalculatingMode calculatingMode, int recDepth) {
        return (double) corpus.getAbsolute(sequence.firstWord())
                / corpus.getNumWords();
    }

}
