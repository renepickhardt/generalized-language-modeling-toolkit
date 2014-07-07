package de.typology.smoothing;

import java.util.List;

import de.typology.utils.StringUtils;

public abstract class InterpolEstimator extends Estimator {

    private Estimator alpha;

    private Estimator beta;

    public InterpolEstimator(
            Corpus corpus,
            Estimator alpha) {
        super(corpus);
        this.alpha = alpha;
        beta = this;
    }

    public InterpolEstimator(
            Corpus corpus,
            Estimator alpha,
            Estimator beta) {
        super(corpus);
        this.alpha = alpha;
        this.beta = beta;
    }

    @Override
    public double propabilityCond(
            List<String> reqSequence,
            List<String> condSequence,
            int recDepth) {
        if (condSequence.isEmpty()) {
            // TODO: Rene: marker for double check
            return alpha.propabilityCond(reqSequence, condSequence);
        }

        List<String> condSequence2 =
                condSequence.subList(1, condSequence.size());

        double lambdaVal = lambda(reqSequence, condSequence2, recDepth);
        double alphaVal =
                alpha.propabilityCond(reqSequence, condSequence, recDepth);
        double betaVal =
                beta.propabilityCond(reqSequence, condSequence2, recDepth);
        double result = lambdaVal * alphaVal + (1 - lambdaVal) * betaVal;
        logger.debug(StringUtils.repeat("  ", recDepth) + "lambda("
                + reqSequence + ", " + condSequence + ") = " + lambdaVal);
        logger.debug(StringUtils.repeat("  ", recDepth) + "alpha("
                + reqSequence + ", " + condSequence + ") = " + alphaVal);
        logger.debug(StringUtils.repeat("  ", recDepth) + "beta(" + reqSequence
                + ", " + condSequence2 + ") = " + betaVal);
        logger.debug(StringUtils.repeat("  ", recDepth)
                + "returning lambdaVal * alphaVal + (1 - lambdaVal) * betaVal = "
                + result);
        return result;
    }

    protected abstract double lambda(
            List<String> reqSequence,
            List<String> condSequence,
            int recDepth);

}
