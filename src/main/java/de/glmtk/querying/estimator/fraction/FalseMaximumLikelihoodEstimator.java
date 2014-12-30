package de.glmtk.querying.estimator.fraction;

import de.glmtk.common.NGram;

// TODO: Rene: You told me this estimator should work for Marginal
// Probabilities. It doesn't, why?
public class FalseMaximumLikelihoodEstimator extends FractionEstimator {

    @Override
    protected double calcNumerator(NGram sequence, NGram history, int recDepth) {
        NGram fullSequence = getFullSequence(sequence, history);
        long fullSequenceCount = countCache.getAbsolute(fullSequence);
        logDebug(recDepth, "fullSequence = %s (%d)", fullSequence,
                fullSequenceCount);
        return fullSequenceCount;
    }

    @Override
    protected double
        calcDenominator(NGram sequence, NGram history, int recDepth) {
        long historyCount;
        if (history.isEmpty()) {
            historyCount = countCache.getNumWords();
        } else {
            historyCount = countCache.getAbsolute(history);
        }
        logDebug(recDepth, "fullHistory = %s (%d)", history, historyCount);
        return historyCount;
    }
}
