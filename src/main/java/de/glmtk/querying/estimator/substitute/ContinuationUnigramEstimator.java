package de.glmtk.querying.estimator.substitute;

import static de.glmtk.utils.NGram.WSKP_NGRAM;
import de.glmtk.utils.NGram;

public class ContinuationUnigramEstimator extends SubstituteEstimator {

    private static final NGram WSKP_WSKP_NGRAM = WSKP_NGRAM.concat(WSKP_NGRAM);

    @Override
    protected double
        calcProbability(NGram sequence, NGram history, int recDepth) {
        return (double) countCache.getContinuation(
                WSKP_NGRAM.concat(sequence.get(0).convertSkpToWskp()))
                .getOnePlusCount()
                / countCache.getContinuation(WSKP_WSKP_NGRAM).getOnePlusCount();
    }
}
