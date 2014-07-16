package de.glmtk.smoothing;

import java.util.List;

import de.glmtk.utils.StringUtils;

public abstract class InterpolEstimator extends Estimator {

    private Estimator alpha;

    private Estimator beta;

    public InterpolEstimator(
            Estimator alpha) {
        this.alpha = alpha;
        beta = this;
    }

    public InterpolEstimator(
            Estimator alpha,
            Estimator beta) {
        this.alpha = alpha;
        this.beta = beta;
    }

    @Override
    public void setCorpus(Corpus corpus) {
        super.setCorpus(corpus);
        if (alpha != this) {
            alpha.setCorpus(corpus);
        }
        if (beta != this) {
            beta.setCorpus(corpus);
        }
    }

    @Override
    protected double propabilityCond(
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
