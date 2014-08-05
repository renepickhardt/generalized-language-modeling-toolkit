package de.glmtk.smoothing.estimating;

import de.glmtk.smoothing.NGram;

/**
 * {@link FalseMaximumLikelihoodEstimator} is a marginal probability.
 */
public class FalseMaximumLikelihoodEstimator extends FractionEstimator {

    @Override
    protected double calcNumerator(NGram sequence, NGram history, int recDepth) {
        NGram fullSequence = getFullSequence(sequence, history);
        int fullSequenceCount = corpus.getAbsolute(fullSequence);
        logDebug(recDepth, "fullSequence = {} ({})", fullSequence,
                fullSequenceCount);
        return fullSequenceCount;
    }

    @Override
    protected double
        calcDenominator(NGram sequence, NGram history, int recDepth) {
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
