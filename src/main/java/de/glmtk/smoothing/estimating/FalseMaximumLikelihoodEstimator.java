package de.glmtk.smoothing.estimating;

import de.glmtk.smoothing.NGram;

/**
 * {@code P_FMLE(s | h) = c(fs) / c(h)}
 */
public class FalseMaximumLikelihoodEstimator extends FractionEstimator {

    @Override
    protected double numerator(NGram sequence, NGram history, int recDepth) {
        NGram fullSequence = getFullSequence(sequence, history);
        int fullSequenceCount = corpus.getAbsolute(fullSequence);
        logDebug(recDepth, "fullSequence = {} ({})", fullSequence,
                fullSequenceCount);
        return fullSequenceCount;
    }

    @Override
    protected double denominator(NGram sequence, NGram history, int recDepth) {
        int historyCount;
        if (history.isEmpty()) {
            historyCount = corpus.getNumWords();
        } else {
            historyCount = corpus.getAbsolute(history);
        }
        logDebug(recDepth, "history = {} ({})", history, historyCount);
        return historyCount;
    }

}
