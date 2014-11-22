package de.glmtk.smoothing.estimator.discount;

import de.glmtk.smoothing.estimator.fraction.FractionEstimator;
import de.glmtk.utils.NGram;

public class AbsoluteDiscountEstimator extends DiscountEstimator {

    private double discount;

    public AbsoluteDiscountEstimator(
            FractionEstimator fractionEstimator,
            double discount) {
        super(fractionEstimator);
        this.discount = discount;
    }

    @Override
    protected double calcDiscount(NGram sequence, NGram history, int recDepth) {
        return discount;
    }

}
