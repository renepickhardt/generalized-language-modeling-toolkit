package de.glmtk.smoothing.estimating;

import de.glmtk.patterns.PatternElem;
import de.glmtk.smoothing.CalculatingMode;
import de.glmtk.smoothing.Corpus;
import de.glmtk.smoothing.NGram;

// TODO: Why does SkipCalculator SequenceSum Test fail for n=5?
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
    public void setCalculatingMode(CalculatingMode calculatingMode) {
        super.setCalculatingMode(calculatingMode);
        alpha.setCalculatingMode(calculatingMode);
        if (beta != this) {
            beta.setCalculatingMode(calculatingMode);
        }
    }

    @Override
    protected double
        calcProbability(NGram sequence, NGram history, int recDepth) {
        // TODO: Rene: can we really just backoff how far we want here without worry?
        // TODO: Test what yields better entropy: substitute or backoff?
        NGram backoffHistory = history.backoffUntilSeen(corpus);
        if (backoffHistory.isEmpty()) {
            logDebug(recDepth, "history empty, substituting:");
            return SUBSTITUTE_ESTIMATOR
                    .probability(sequence, history, recDepth);
        } else {
            double alphaVal = alpha.probability(sequence, history, recDepth);
            double betaVal =
                    beta.probability(sequence, backoffHistory, recDepth);
            double gammaVal = gamma(sequence, history, recDepth);

            return alphaVal + gammaVal * betaVal;
        }
    }

    protected double gamma(NGram sequence, NGram history, int recDepth) {
        logDebug(recDepth, "gamma({}, {})", sequence, history);
        ++recDepth;

        double discount = alpha.discount(sequence, history, recDepth);
        double denominator = alpha.denominator(sequence, history, recDepth);
        double n_1p =
                corpus.getContinuation(history.concat(PatternElem.SKIPPED_WORD))
                        .getOnePlusCount();

        if (denominator == 0) {
            logDebug(recDepth, "denominator = 0, setting gamma = 0:");
            return 0;
        } else {
            logDebug(recDepth, "n_1p = " + n_1p);
            return discount * n_1p / denominator;
        }
    }

}
