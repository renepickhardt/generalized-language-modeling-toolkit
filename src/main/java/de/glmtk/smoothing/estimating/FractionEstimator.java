package de.glmtk.smoothing.estimating;

import de.glmtk.smoothing.NGram;

/**
 * {@code P_Frac(s|h) [ n ; d ] = if (h unseen || d == 0) P_Substitute(s|h) else n / d}
 */
public abstract class FractionEstimator extends Estimator {

    @Override
    protected double
        calcPropability(NGram sequence, NGram history, int recDepth) {
        // if history is unseen (empty history counts as seen)
        if (!history.isEmpty() && corpus.getAbsolute(history) == 0) {
            // TODO: Rene: Tests only work if we substitute here, but this goes
            // against what you tried to prove to me. Why is returning 0 wrong?
            logDebug(recDepth, "history unseen, substituting:");
            return SUBSTITUTE_ESTIMATOR
                    .propability(sequence, history, recDepth);
            // logDebug(recDepth, "history unseen, returning 0");
            // return 0;
        }

        double numeratorVal = numerator(sequence, history, recDepth + 1);
        double denominatorVal = denominator(sequence, history, recDepth + 1);
        if (denominatorVal == 0) {
            // TODO: Rene: Tests only work if we substitute here, but this goes
            // against what you tried to prove to me. Why is returning 0 wrong?
            logDebug(recDepth, "denominator = 0; substituting:");
            return SUBSTITUTE_ESTIMATOR
                    .propability(sequence, history, recDepth);
            //            logDebug(recDepth, "denominator = 0; returning 0");
            //            return 0;
        } else {
            logDebug(recDepth, "numerator = {}", numeratorVal);
            logDebug(recDepth, "denominator = {}", denominatorVal);
            return numeratorVal / denominatorVal;
        }
    }

    protected abstract double numerator(
            NGram sequence,
            NGram history,
            int recDepth);

    protected abstract double denominator(
            NGram sequence,
            NGram history,
            int recDepth);

}
