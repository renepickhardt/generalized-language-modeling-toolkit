package de.glmtk.smoothing.estimator.fraction;

import de.glmtk.smoothing.NGram;

// TODO: Rene: You told me this estimator should work for Marginal
// Probabilities. It doesn't, why?
public class FalseMaximumLikelihoodEstimator extends FractionEstimator {

    @Override
    protected double calcNumerator(NGram sequence, NGram history, int recDepth) {
        NGram fullSequence = getFullSequence(sequence, history);
        long fullSequenceCount = countCache.getAbsolute(fullSequence);
        logDebug(recDepth, "fullSequence = {} ({})", fullSequence,
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
        logDebug(recDepth, "fullHistory = {} ({})", history, historyCount);
        return historyCount;
    }
}
