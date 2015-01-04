package de.glmtk.querying.estimator.combination;

import de.glmtk.common.CountCache;
import de.glmtk.common.NGram;
import de.glmtk.common.ProbMode;
import de.glmtk.querying.estimator.Estimator;

public class CombinationEstimator extends Estimator {
    private double lambda;
    private Estimator alpha;
    private Estimator beta;

    public CombinationEstimator(Estimator alpha,
                                Estimator beta,
                                double lambda) {
        this.alpha = alpha;
        this.beta = beta;
        this.lambda = lambda;
    }

    @Override
    public void setCountCache(CountCache countCache) {
        super.setCountCache(countCache);
        alpha.setCountCache(countCache);
        beta.setCountCache(countCache);
    }

    @Override
    public void setProbMode(ProbMode probMode) {
        super.setProbMode(probMode);
        alpha.setProbMode(probMode);
        beta.setProbMode(probMode);
    }

    @Override
    protected double calcProbability(NGram sequence,
                                     NGram history,
                                     int recDepth) {
        double alphaVal = alpha.probability(sequence, history, recDepth);
        double betaVal = beta.probability(sequence, history, recDepth);
        logTrace(recDepth, "alpha = %f", alphaVal);
        logTrace(recDepth, "beta = %f", betaVal);
        return lambda * alphaVal + (1.0 - lambda) * betaVal;
    }
}
