package de.glmtk.smoothing.estimator.interpolation;

import de.glmtk.Counter;
import de.glmtk.pattern.Pattern;
import de.glmtk.smoothing.Corpus;
import de.glmtk.smoothing.NGram;
import de.glmtk.smoothing.ProbMode;
import de.glmtk.smoothing.estimator.Estimator;
import de.glmtk.smoothing.estimator.discount.DiscountEstimator;
import de.glmtk.smoothing.estimator.discount.ModifiedKneserNeyDiscountEstimator;

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
            NGram historyWithSkip = history.concat(NGram.SKIPPED_WORD_NGRAM);
            if (alpha.getClass() == ModifiedKneserNeyDiscountEstimator.class) {
                ModifiedKneserNeyDiscountEstimator a =
                        (ModifiedKneserNeyDiscountEstimator) alpha;
                Pattern pattern = history.toPattern();
                double d1 = a.getDiscount1(pattern);
                double d2 = a.getDiscount2(pattern);
                double d3p = a.getDiscount3p(pattern);

                Counter continuation = corpus.getContinuation(historyWithSkip);
                double n1 = continuation.getOneCount();
                double n2 = continuation.getTwoCount();
                double n3p = continuation.getThreePlusCount();

                logDebug(recDepth, "pattern = {}", pattern);
                logDebug(recDepth, "d1      = {}", d1);
                logDebug(recDepth, "d2      = {}", d2);
                logDebug(recDepth, "d3p     = {}", d3p);
                logDebug(recDepth, "n1      = {}", n1);
                logDebug(recDepth, "n2      = {}", n2);
                logDebug(recDepth, "n3p     = {}", n3p);

                return (d1 * n1 + d2 * n2 * d3p * n3p) / denominator;
            } else {
                double discout = alpha.discount(sequence, history, recDepth);
                double n_1p =
                        corpus.getContinuation(historyWithSkip)
                                .getOnePlusCount();

                logDebug(recDepth, "denominator = {}", denominator);
                logDebug(recDepth, "discount = {}", discout);
                logDebug(recDepth, "n_1p = {}", n_1p);

                return discout * n_1p / denominator;
            }
        }
    }

}
