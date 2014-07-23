package de.glmtk.smoothing.estimating;

import de.glmtk.smoothing.NGram;

/**
 * {@code P_Frac(s|h) [ n , d ] = if (h unseen || d == 0) P_Substitute(s|h) else n / d}
 */
public abstract class FractionEstimator extends Estimator {

    @Override
    protected double
        calcProbability(NGram sequence, NGram history, int recDepth) {
        // if history is unseen (empty history counts as seen)
        if (!history.isEmpty() && corpus.getAbsolute(history) == 0) {
            // TODO: Rene: Tests only work if we substitute here, but this goes
            // against what you tried to prove to me. Why is returning 0 wrong?
            logDebug(recDepth, "history unseen, substituting:");
            return SUBSTITUTE_ESTIMATOR
                    .probability(sequence, history, recDepth);
            // logDebug(recDepth, "history unseen, returning 0");
            // return 0;
        }

        double numeratorVal = numerator(sequence, history, recDepth);
        double denominatorVal = denominator(sequence, history, recDepth);
        if (denominatorVal == 0) {
            // TODO: Rene: Tests only work if we substitute here, but this goes
            // against what you tried to prove to me. Why is returning 0 wrong?
            logDebug(recDepth, "denominator = 0, substituting:");
            return SUBSTITUTE_ESTIMATOR
                    .probability(sequence, history, recDepth);
            //            logDebug(recDepth, "denominator = 0; returning 0");
            //            return 0;
        } else {
            return numeratorVal / denominatorVal;
        }
    }

    /**
     * Wrapper around {@link #calcNumerator(NGram, NGram, int)} to add logging.
     */
    protected final double
        numerator(NGram sequence, NGram history, int recDepth) {
        logDebug(recDepth, "numerator({},{})", sequence, history);
        ++recDepth;

        double result = calcNumerator(sequence, history, recDepth);
        logDebug(recDepth, "result = {}", result);
        return result;
    }

    protected abstract double calcNumerator(
            NGram sequence,
            NGram history,
            int recDepth);

    /**
     * Wrapper around {@link #calcDenominator(NGram, NGram, int)} to add
     * logging.
     */
    protected final double denominator(
            NGram sequence,
            NGram history,
            int recDepth) {
        logDebug(recDepth, "denominator({},{})", sequence, history);
        ++recDepth;

        double result = calcDenominator(sequence, history, recDepth);
        logDebug(recDepth, "result = {}", result);
        return result;
    }

    protected abstract double calcDenominator(
            NGram sequence,
            NGram history,
            int recDepth);

}