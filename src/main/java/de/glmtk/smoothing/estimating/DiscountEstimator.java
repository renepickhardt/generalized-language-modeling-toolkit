package de.glmtk.smoothing.estimating;

import de.glmtk.smoothing.Corpus;
import de.glmtk.smoothing.NGram;

/**
 * {@link DiscountEstimator}s do not return a probability distribution.
 * 
 * {@code P_Frac(s | h) [ D , P_Frac [ n , d ] ] = P_Frac [ n = max(0, n - D) ; d = d ]

 */
public abstract class DiscountEstimator extends FractionEstimator {

    private FractionEstimator fractionEstimator;

    public DiscountEstimator(
            FractionEstimator fractionEstimator) {
        this.fractionEstimator = fractionEstimator;
    }

    @Override
    public void setCorpus(Corpus corpus) {
        super.setCorpus(corpus);
        fractionEstimator.setCorpus(corpus);
    }

    //    /**
    //     * Implementation according to
    //     * {@link FractionEstimator#calcProbability(NGram, NGram, int)}.
    //     */
    //    @Override
    //    protected double
    //        calcProbability(NGram sequence, NGram history, int recDepth) {
    //        if (!history.isEmpty() && corpus.getAbsolute(history) == 0) {
    //            logDebug(recDepth, "history unseen, substituting:");
    //            return SUBSTITUTE_ESTIMATOR
    //                    .probability(sequence, history, recDepth);
    //        }
    //
    //        double numeratorVal =
    //                fractionEstimator.numerator(sequence, history, recDepth);
    //        double denominatorVal =
    //                fractionEstimator.numerator(sequence, history, recDepth);
    //        double discountVal = discount(sequence, history, recDepth);
    //        if (denominatorVal == 0) {
    //            logDebug(recDepth, "history unseen, substituting:");
    //            return SUBSTITUTE_ESTIMATOR
    //                    .probability(sequence, history, recDepth);
    //        } else {
    //            if (discountVal > numeratorVal) {
    //                return 0;
    //            } else {
    //                return (numeratorVal - discountVal) / denominatorVal;
    //            }
    //        }
    //    }

    /**
     * Wrapper around {@link #calcDiscount(NGram, NGram, int)} to add logging.
     */
    protected final double
        discount(NGram sequence, NGram history, int recDepth) {
        logDebug(recDepth, "discount({},{})", sequence, history);
        ++recDepth;

        double result = calcDiscount(sequence, history, recDepth);
        logDebug(recDepth, "result = {}", result);
        return result;
    }

    protected abstract double calcDiscount(
            NGram sequence,
            NGram history,
            int recDepth);

    @Override
    protected double calcNumerator(NGram sequence, NGram history, int recDepth) {
        double numeratorVal =
                fractionEstimator.numerator(sequence, history, recDepth);
        double discountVal = discount(sequence, history, recDepth);

        if (discountVal > numeratorVal) {
            return 0;
        } else {
            return numeratorVal - discountVal;
        }
    }

    @Override
    protected double
        calcDenominator(NGram sequence, NGram history, int recDepth) {
        return fractionEstimator.denominator(sequence, history, recDepth);
    }

}
