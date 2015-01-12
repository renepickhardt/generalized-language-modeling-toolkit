package de.glmtk.querying.estimator.interpol;

import static de.glmtk.common.PatternElem.WSKP_WORD;
import de.glmtk.common.BackoffMode;
import de.glmtk.common.CountCache;
import de.glmtk.common.Counts;
import de.glmtk.common.NGram;
import de.glmtk.common.Pattern;
import de.glmtk.common.ProbMode;
import de.glmtk.querying.estimator.Estimator;
import de.glmtk.querying.estimator.discount.DiscountEstimator;
import de.glmtk.querying.estimator.discount.ModifiedKneserNeyDiscountEstimator;

public class InterpolEstimator extends Estimator {
    protected DiscountEstimator alpha;
    protected Estimator beta;
    protected BackoffMode backoffMode;

    public InterpolEstimator(DiscountEstimator alpha,
                             Estimator beta) {
        this(alpha, beta, BackoffMode.DEL);
    }

    public InterpolEstimator(DiscountEstimator alpha) {
        this(alpha, BackoffMode.DEL);
    }

    public InterpolEstimator(DiscountEstimator alpha,
                             BackoffMode backoffMode) {
        this.alpha = alpha;
        beta = this;
        setBackoffMode(backoffMode);
    }

    public InterpolEstimator(DiscountEstimator alpha,
                             Estimator beta,
                             BackoffMode backoffMode) {
        this.alpha = alpha;
        this.beta = beta;
        setBackoffMode(backoffMode);
    }

    @Override
    public void setCountCache(CountCache countCache) {
        super.setCountCache(countCache);
        alpha.setCountCache(countCache);
        if (beta != this)
            beta.setCountCache(countCache);
    }

    @Override
    public void setProbMode(ProbMode probMode) {
        super.setProbMode(probMode);
        alpha.setProbMode(probMode);
        if (beta != this)
            beta.setProbMode(probMode);
    }

    public void setBackoffMode(BackoffMode backoffMode) {
        if (backoffMode != BackoffMode.DEL && backoffMode != BackoffMode.SKP)
            throw new IllegalArgumentException(
                    "Illegal BackoffMode for this class.");
        this.backoffMode = backoffMode;
    }

    @Override
    protected double calcProbability(NGram sequence,
                                     NGram history,
                                     int recDepth) {
        if (history.isEmptyOrOnlySkips()) {
            //if (history.isEmpty()) {
            logTrace(recDepth,
                    "history empty, returning fraction estimator probability");
            return alpha.getFractionEstimator().probability(sequence, history,
                    recDepth);
        }

        NGram backoffHistory = history.backoffUntilSeen(backoffMode, countCache);
        double alphaVal = alpha.probability(sequence, history, recDepth);
        double betaVal = beta.probability(sequence, backoffHistory, recDepth);
        double gammaVal = gamma(sequence, history, recDepth);

        return alphaVal + gammaVal * betaVal;
    }

    public final double gamma(NGram sequence,
                              NGram history,
                              int recDepth) {
        logTrace(recDepth, "gamma(%s,%s)", sequence, history);
        ++recDepth;

        double result = calcGamma(sequence, history, recDepth);
        logTrace(recDepth, "result = %f", result);
        return result;
    }

    protected double calcGamma(NGram sequence,
                               NGram history,
                               int recDepth) {
        double denominator = alpha.denominator(sequence, history, recDepth);

        if (denominator == 0) {
            logTrace(recDepth, "denominator = 0, setting gamma = 0:");
            return 0;
        }

        NGram historyPlusWskp = history.concat(WSKP_WORD);
        if (alpha.getClass() == ModifiedKneserNeyDiscountEstimator.class) {
            ModifiedKneserNeyDiscountEstimator a = (ModifiedKneserNeyDiscountEstimator) alpha;
            Pattern pattern = history.getPattern();
            double[] d = a.getDiscounts(pattern);
            double d1 = d[0];
            double d2 = d[1];
            double d3p = d[2];

            Counts continuation = countCache.getContinuation(historyPlusWskp);
            double n1 = continuation.getOneCount();
            double n2 = continuation.getTwoCount();
            double n3p = continuation.getThreePlusCount();

            logTrace(recDepth, "pattern = %s", pattern);
            logTrace(recDepth, "d1      = %f", d1);
            logTrace(recDepth, "d2      = %f", d2);
            logTrace(recDepth, "d3p     = %f", d3p);
            logTrace(recDepth, "n1      = %f", n1);
            logTrace(recDepth, "n2      = %f", n2);
            logTrace(recDepth, "n3p     = %f", n3p);

            return (d1 * n1 + d2 * n2 + d3p * n3p) / denominator;
        }

        double discout = alpha.discount(sequence, history, recDepth);
        double n_1p = countCache.getContinuation(historyPlusWskp).getOnePlusCount();

        logTrace(recDepth, "denominator = %f", denominator);
        logTrace(recDepth, "discount = %f", discout);
        logTrace(recDepth, "n_1p = %f", n_1p);

        return discout * n_1p / denominator;
    }
}
