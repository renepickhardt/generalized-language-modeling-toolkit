package de.glmtk.smoothing.estimating;

import de.glmtk.patterns.PatternElem;
import de.glmtk.smoothing.NGram;

/**
 * {@code P_ContUnigram(s | h) = P_Frac(s | h) = [ n = N_1p(_ s_1) , d =  N_1p(_ _) ]}
 */
public class ContinuationUnigramEstimator extends Estimator {

    @Override
    protected double
        calcProbability(NGram sequence, NGram history, int recDepth) {
        return (double) corpus
                .getContinuation(
                        new NGram(PatternElem.SKIPPED_WORD).concat(sequence
                                .firstWord())).getOnePlusCount()
                / corpus.getContinuation(
                        new NGram(PatternElem.SKIPPED_WORD)
                                .concat(PatternElem.SKIPPED_WORD))
                        .getOnePlusCount();
    }
}
