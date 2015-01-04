package de.glmtk.querying.estimator.fraction;

import de.glmtk.common.NGram;

public class MaximumLikelihoodEstimator extends FractionEstimator {
    @Override
    protected double calcNumerator(NGram sequence,
                                   NGram history,
                                   int recDepth) {
        NGram fullSequence = getFullSequence(sequence, history);
        long fullSequenceCount = countCache.getAbsolute(fullSequence);
        logTrace(recDepth, "fullSequence = %s (%d)", fullSequence,
                fullSequenceCount);
        return fullSequenceCount;
    }

    @Override
    protected double calcDenominator(NGram sequence,
                                     NGram history,
                                     int recDepth) {
        NGram fullHistory = getFullHistory(sequence, history);
        long fullHistoryCount = countCache.getAbsolute(fullHistory);
        logTrace(recDepth, "fullHistory = %s (%d)", fullHistory,
                fullHistoryCount);
        return fullHistoryCount;
    }
}
