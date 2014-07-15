package de.typology.smoothing;

import java.util.List;

public class AbsoluteInterpolEstimator extends InterpolEstimator {

    private double lambda;

    public AbsoluteInterpolEstimator(
            Estimator alpha,
            double lambda) {
        super(alpha);
        this.lambda = lambda;
    }

    public AbsoluteInterpolEstimator(
            Estimator alpha,
            Estimator beta,
            double lambda) {
        super(alpha, beta);
        this.lambda = lambda;
    }

    @Override
    protected double lambda(
            List<String> reqSequence,
            List<String> condSequence,
            int recDepth) {
        return lambda;
    }

}
