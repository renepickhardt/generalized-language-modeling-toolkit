package de.glmtk.smoothing.estimating;

import de.glmtk.smoothing.CalculatingMode;
import de.glmtk.smoothing.Corpus;
import de.glmtk.smoothing.NGram;

/**
 * {@link DiscountEstimator}s do not return a probability distribution.
 * 
 * Currently only usable in combination with {@link InterpolationEstimator}.
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

    @Override
    public void setCalculatingMode(CalculatingMode calculatingMode) {
        super.setCalculatingMode(calculatingMode);
        fractionEstimator.setCalculatingMode(calculatingMode);
    }

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
