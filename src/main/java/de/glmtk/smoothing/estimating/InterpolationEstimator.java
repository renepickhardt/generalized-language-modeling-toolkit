package de.glmtk.smoothing.estimating;

import de.glmtk.patterns.PatternElem;
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

    @Override
    public void setCorpus(Corpus corpus) {
        super.setCorpus(corpus);
        alpha.setCorpus(corpus);
        beta.setCorpus(corpus);
    }

    @Override
    protected double
        calcProbability(NGram sequence, NGram history, int recDepth) {
        // TODO: Test what yields better entropy: substitute or backoff?
        history = backoffUntilSeen(history);
        if (history.isEmpty()) {
            logDebug(recDepth, "history empty, substituting:");
            return SUBSTITUTE_ESTIMATOR
                    .probability(sequence, history, recDepth);
        } else {
            double alphaVal = alpha.probability(sequence, history, recDepth);
            double betaVal = beta.probability(sequence, history, recDepth);
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
