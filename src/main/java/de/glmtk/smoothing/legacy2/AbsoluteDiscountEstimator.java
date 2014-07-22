package de.glmtk.smoothing.legacy2;

import java.util.List;

public class AbsoluteDiscountEstimator extends DiscountEstimator {

    private double discount;

    public AbsoluteDiscountEstimator(
            FractionEstimator fractionEstimator,
            double discount) {
        super(fractionEstimator);
        this.discount = discount;
    }

    @Override
    protected double discount(
            List<String> reqSequence,
            List<String> condSequence,
            int recDepth) {
        return discount;
    }

}
