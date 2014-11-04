package de.glmtk.smoothing.estimator.fraction;

import de.glmtk.smoothing.NGram;

public class MaximumLikelihoodEstimator extends FractionEstimator {

    @Override
    protected double calcNumerator(NGram sequence, NGram history, int recDepth) {
        NGram fullSequence = getFullSequence(sequence, history);
        int fullSequenceCount = countCache.getAbsolute(fullSequence);
        logDebug(recDepth, "fullSequence = {} ({})", fullSequence,
                fullSequenceCount);
        return fullSequenceCount;
    }

    @Override
    protected double
        calcDenominator(NGram sequence, NGram history, int recDepth) {
        NGram fullHistory = getFullHistory(sequence, history);
        int fullHistoryCount = countCache.getAbsolute(fullHistory);
        logDebug(recDepth, "fullHistory = {} ({})", fullHistory,
                fullHistoryCount);
        return fullHistoryCount;
    }

}
