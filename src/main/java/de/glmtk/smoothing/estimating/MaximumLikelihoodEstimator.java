package de.glmtk.smoothing.estimating;

import de.glmtk.smoothing.NGram;

/**
 * {@code P_MLE(s | h) = c(fs) / c(fh)}
 */
public class MaximumLikelihoodEstimator extends FractionEstimator {

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
        NGram fullHistory = getFullHistory(sequence, history);
        int fullHistoryCount;
        if (fullHistory.isEmpty()) {
            fullHistoryCount = corpus.getNumWords();
        } else {
            fullHistoryCount = corpus.getAbsolute(fullHistory);
        }
        logDebug(recDepth, "fullHistory = {} ({})", fullHistory,
                fullHistoryCount);
        return fullHistoryCount;
    }

}
