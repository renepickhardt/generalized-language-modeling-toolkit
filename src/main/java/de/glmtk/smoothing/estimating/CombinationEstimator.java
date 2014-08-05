package de.glmtk.smoothing.estimating;

import de.glmtk.smoothing.CalculatingMode;
import de.glmtk.smoothing.Corpus;
import de.glmtk.smoothing.NGram;

public class CombinationEstimator extends Estimator {

    private double lambda;

    private Estimator alpha;

    private Estimator beta;

    public CombinationEstimator(
            Estimator alpha,
            Estimator beta,
            double lambda) {
        this.alpha = alpha;
        this.beta = beta;
        this.lambda = lambda;
    }

    @Override
    public void setCorpus(Corpus corpus) {
        super.setCorpus(corpus);
        alpha.setCorpus(corpus);
        beta.setCorpus(corpus);
    }

    @Override
    protected double calcProbability(
            NGram sequence,
            NGram history,
            CalculatingMode calculatingMode,
            int recDepth) {
        double alphaVal =
                alpha.probability(sequence, history, calculatingMode, recDepth);
        double betaVal =
                beta.probability(sequence, history, calculatingMode, recDepth);
        logDebug(recDepth, "alpha = {}", alphaVal);
        logDebug(recDepth, "beta = {}", betaVal);
        return lambda * alphaVal * (1.0 - lambda) * betaVal;
    }

}
