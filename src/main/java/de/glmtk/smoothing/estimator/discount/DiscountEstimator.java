package de.glmtk.smoothing.estimator.discount;

import de.glmtk.smoothing.CountCache;
import de.glmtk.smoothing.NGram;
import de.glmtk.smoothing.ProbMode;
import de.glmtk.smoothing.estimator.fraction.FractionEstimator;

public abstract class DiscountEstimator extends FractionEstimator {

    private FractionEstimator fractionEstimator;

    public DiscountEstimator(
            FractionEstimator fractionEstimator) {
        this.fractionEstimator = fractionEstimator;
    }

    // TODO: hack
    public FractionEstimator getFractionEstimator() {
        return fractionEstimator;
    }

    @Override
    public void setCorpus(CountCache countCache) {
        super.setCorpus(countCache);
        fractionEstimator.setCorpus(countCache);
    }

    @Override
    public void setProbMode(ProbMode probMode) {
        super.setProbMode(probMode);
        fractionEstimator.setProbMode(probMode);
    }

    public final double discount(NGram sequence, NGram history, int recDepth) {
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
