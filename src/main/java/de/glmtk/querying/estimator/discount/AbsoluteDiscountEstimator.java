package de.glmtk.querying.estimator.discount;

import de.glmtk.common.NGram;
import de.glmtk.querying.estimator.fraction.FractionEstimator;

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
