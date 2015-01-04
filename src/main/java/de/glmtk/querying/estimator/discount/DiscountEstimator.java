package de.glmtk.querying.estimator.discount;

import de.glmtk.common.CountCache;
import de.glmtk.common.NGram;
import de.glmtk.common.ProbMode;
import de.glmtk.querying.estimator.fraction.FractionEstimator;

public abstract class DiscountEstimator extends FractionEstimator {
    private FractionEstimator fractionEstimator;

    public DiscountEstimator(FractionEstimator fractionEstimator) {
        this.fractionEstimator = fractionEstimator;
    }

    // TODO: hack
    public FractionEstimator getFractionEstimator() {
        return fractionEstimator;
    }

    @Override
    public void setCountCache(CountCache countCache) {
        super.setCountCache(countCache);
        fractionEstimator.setCountCache(countCache);
    }

    @Override
    public void setProbMode(ProbMode probMode) {
        super.setProbMode(probMode);
        fractionEstimator.setProbMode(probMode);
    }

    public final double discount(NGram sequence,
                                 NGram history,
                                 int recDepth) {
        logTrace(recDepth, "discount(%s,%s)", sequence, history);
        ++recDepth;

        double result = calcDiscount(sequence, history, recDepth);
        logTrace(recDepth, "result = %f", result);
        return result;
    }

    protected abstract double calcDiscount(NGram sequence,
                                           NGram history,
                                           int recDepth);

    @Override
    protected double calcNumerator(NGram sequence,
                                   NGram history,
                                   int recDepth) {
        double numeratorVal = fractionEstimator.numerator(sequence, history,
                recDepth);
        double discountVal = discount(sequence, history, recDepth);

        if (discountVal > numeratorVal)
            return 0;
        else
            return numeratorVal - discountVal;
    }

    @Override
    protected double calcDenominator(NGram sequence,
                                     NGram history,
                                     int recDepth) {
        return fractionEstimator.denominator(sequence, history, recDepth);
    }
}
