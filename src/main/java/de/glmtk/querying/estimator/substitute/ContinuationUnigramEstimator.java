package de.glmtk.querying.estimator.substitute;

import de.glmtk.utils.NGram;

public class ContinuationUnigramEstimator extends SubstituteEstimator {

    @Override
    protected double
        calcProbability(NGram sequence, NGram history, int recDepth) {
        return (double) countCache.getContinuation(
                NGram.SKP_NGRAM.concat(sequence.get(0)))
                .getOnePlusCount()
                / countCache.getContinuation(
                        NGram.SKP_NGRAM
                                .concat(NGram.SKP_NGRAM))
                        .getOnePlusCount();
    }
}
