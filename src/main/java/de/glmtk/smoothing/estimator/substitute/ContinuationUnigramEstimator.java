package de.glmtk.smoothing.estimator.substitute;

import de.glmtk.smoothing.NGram;

public class ContinuationUnigramEstimator extends SubstituteEstimator {

    @Override
    protected double
        calcProbability(NGram sequence, NGram history, int recDepth) {
        return (double) countCache.getContinuation(
                NGram.SKIPPED_WORD_NGRAM.concat(sequence.get(0)))
                .getOnePlusCount()
                / countCache.getContinuation(
                        NGram.SKIPPED_WORD_NGRAM
                                .concat(NGram.SKIPPED_WORD_NGRAM))
                        .getOnePlusCount();
    }
}
