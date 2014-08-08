package de.glmtk.smoothing.legacy3.estimating;

import de.glmtk.patterns.PatternElem;
import de.glmtk.smoothing.NGram;

public class ContinuationUnigramEstimator extends Estimator {

    @Override
    protected double
        calcProbability(NGram sequence, NGram history, int recDepth) {
        return (double) corpus.getContinuation(
                new NGram(PatternElem.SKIPPED_WORD).concat(sequence.get(0)))
                .getOnePlusCount()
                / corpus.getContinuation(
                        new NGram(PatternElem.SKIPPED_WORD)
                                .concat(PatternElem.SKIPPED_WORD))
                        .getOnePlusCount();
    }

}
