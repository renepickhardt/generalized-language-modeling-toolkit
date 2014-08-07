package de.glmtk.smoothing.legacy3.estimating;

import de.glmtk.smoothing.NGram;

public class AbsoluteDiscountEstimator extends DiscountEstimator {

    private double discount;

    public AbsoluteDiscountEstimator(
            FractionEstimator fractionEstimator,
            double discount) {
        super(fractionEstimator);
        this.discount = discount;
    }

    @Override
    protected double calcDiscount(
            NGram sequence,
            NGram history,
            int recDepth) {
        return discount;
    }

}
