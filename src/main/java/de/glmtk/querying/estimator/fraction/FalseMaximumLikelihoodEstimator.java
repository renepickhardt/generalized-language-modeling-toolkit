package de.glmtk.querying.estimator.fraction;

import de.glmtk.common.NGram;

public class FalseMaximumLikelihoodEstimator extends FractionEstimator {
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
        long historyCount;
        if (history.isEmpty())
            historyCount = countCache.getNumWords();
        else
            historyCount = countCache.getAbsolute(history);
        logTrace(recDepth, "fullHistory = %s (%d)", history, historyCount);
        return historyCount;
    }
}
