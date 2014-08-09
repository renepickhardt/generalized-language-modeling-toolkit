package de.glmtk.smoothing.estimator.interpolation;

import de.glmtk.smoothing.Corpus;
import de.glmtk.smoothing.NGram;
import de.glmtk.smoothing.ProbMode;
import de.glmtk.smoothing.estimator.Estimator;
import de.glmtk.smoothing.estimator.discount.DiscountEstimator;

public class InterpolationEstimator extends Estimator {

    private DiscountEstimator alpha;

    private Estimator beta;

    public InterpolationEstimator(
            DiscountEstimator alpha,
            Estimator beta) {
        this.alpha = alpha;
        this.beta = beta;
    }

    public InterpolationEstimator(
            DiscountEstimator alpha) {
        this.alpha = alpha;
        beta = this;
    }

    @Override
    public void setCorpus(Corpus corpus) {
        super.setCorpus(corpus);
        alpha.setCorpus(corpus);
        if (beta != this) {
            beta.setCorpus(corpus);
        }
    }

    @Override
    public void setProbMode(ProbMode probMode) {
        super.setProbMode(probMode);
        alpha.setProbMode(probMode);
        if (beta != this) {
            beta.setProbMode(probMode);
        }
    }

    @Override
    protected double
        calcProbability(NGram sequence, NGram history, int recDepth) {
        if (history.isEmptyOrOnlySkips()) {
            logDebug(recDepth,
                    "history empty, returning fraction estimator probability");
            return alpha.getFractionEstimator().probability(sequence, history,
                    recDepth);
        } else {
            NGram backoffHistory = history.backoffUntilSeen(probMode, corpus);
            double alphaVal = alpha.probability(sequence, history, recDepth);
            double betaVal =
                    beta.probability(sequence, backoffHistory, recDepth);
            double gammaVal = gamma(sequence, history, recDepth);

            return alphaVal + gammaVal * betaVal;
        }
    }

    public final double gamma(NGram sequence, NGram history, int recDepth) {
        logDebug(recDepth, "gamma({},{})", sequence, history);
        ++recDepth;

        double result = calcGamma(sequence, history, recDepth);
        logDebug(recDepth, "  result = {}", result);
        return result;
    }

    protected double calcGamma(NGram sequence, NGram history, int recDepth) {
        double denominator = alpha.denominator(sequence, history, recDepth);

        if (denominator == 0) {
            logDebug(recDepth, "denominator = 0, setting gamma = 0:");
            return 0;
        } else {
            double discout = alpha.discount(sequence, history, recDepth);
            double n_1p =
                    corpus.getContinuation(
                            history.concat(NGram.SKIPPED_WORD_NGRAM))
                            .getOnePlusCount();
            logDebug(recDepth, "denominator = {}", denominator);
            logDebug(recDepth, "discount = {}", discout);
            logDebug(recDepth, "n_1p = {}", n_1p);
            return discout * n_1p / denominator;
        }
    }

}
