package de.glmtk.smoothing.estimating;

import de.glmtk.patterns.PatternElem;
import de.glmtk.smoothing.NGram;

/**
 * {@code P_CMLE(s | h) = N_1p(fs) / N_1p(fh)}
 */
public class ContinuationMaximumLikelihoodEstimator extends FractionEstimator {

    @Override
    protected double calcNumerator(NGram sequence, NGram history, int recDepth) {
        NGram contFullSequence =
                new NGram(PatternElem.SKIPPED_WORD).concat(getFullSequence(
                        sequence, history));
        long contFullSequenceCount =
                corpus.getContinuation(contFullSequence).getOnePlusCount();
        logDebug(recDepth, "contFullSequence = {} ({})", contFullSequence,
                contFullSequenceCount);
        return contFullSequenceCount;
    }

    @Override
    protected double calcDenominator(NGram sequence, NGram history, int recDepth) {
        NGram contFullHistory =
                new NGram(PatternElem.SKIPPED_WORD).concat(getFullHistory(
                        sequence, history));
        long contFullHistoryCount =
                corpus.getContinuation(contFullHistory).getOnePlusCount();
        logDebug(recDepth, "contFullHistory = {} ({})", contFullHistory,
                contFullHistoryCount);
        return contFullHistoryCount;
    }

}
