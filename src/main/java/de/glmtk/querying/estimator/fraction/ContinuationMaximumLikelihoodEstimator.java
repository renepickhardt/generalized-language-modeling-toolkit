package de.glmtk.querying.estimator.fraction;

import de.glmtk.utils.NGram;

public class ContinuationMaximumLikelihoodEstimator extends FractionEstimator {

    @Override
    protected double calcNumerator(NGram sequence, NGram history, int recDepth) {
        NGram contFullSequence =
                NGram.SKP_NGRAM.concat(getFullSequence(sequence,
                        history));
        long contFullSequenceCount =
                countCache.getContinuation(contFullSequence).getOnePlusCount();
        logDebug(recDepth, "contFullSequence = {} ({})", contFullSequence,
                contFullSequenceCount);
        return contFullSequenceCount;
    }

    @Override
    protected double
        calcDenominator(NGram sequence, NGram history, int recDepth) {
        NGram contFullHistory =
                NGram.SKP_NGRAM.concat(getFullHistory(sequence,
                        history));
        long contFullHistoryCount =
                countCache.getContinuation(contFullHistory).getOnePlusCount();
        logDebug(recDepth, "contFullHistory = {} ({})", contFullHistory,
                contFullHistoryCount);
        return contFullHistoryCount;
    }

}
