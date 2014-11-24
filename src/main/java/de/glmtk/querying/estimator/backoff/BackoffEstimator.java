package de.glmtk.querying.estimator.backoff;

import java.util.HashMap;
import java.util.Map;

import de.glmtk.querying.ProbMode;
import de.glmtk.querying.estimator.Estimator;
import de.glmtk.utils.CountCache;
import de.glmtk.utils.NGram;

public class BackoffEstimator extends Estimator {

    private Map<NGram, Double> gammaCache;

    private Estimator alpha;

    private Estimator beta;

    public BackoffEstimator(
            Estimator alpha) {
        this.alpha = alpha;
        beta = this;
    }

    public BackoffEstimator(
            Estimator alpha,
            Estimator beta) {
        this.alpha = alpha;
        this.beta = beta;
    }

    @Override
    public void setCountCache(CountCache countCache) {
        super.setCountCache(countCache);
        alpha.setCountCache(countCache);
        if (beta != this) {
            beta.setCountCache(countCache);
        }

        gammaCache = new HashMap<NGram, Double>();
    }

    @Override
    public void setProbMode(ProbMode probMode) {
        super.setProbMode(probMode);
        alpha.setProbMode(probMode);
        if (beta != this) {
            beta.setProbMode(probMode);
        }

        gammaCache = new HashMap<NGram, Double>();
    }

    @Override
    protected double
        calcProbability(NGram sequence, NGram history, int recDepth) {
        if (history.isEmpty()) {
            switch (probMode) {
                case COND:
                    return 0;
                case MARG:
                    return SUBSTITUTE_ESTIMATOR.probability(sequence, history,
                            recDepth);
                default:
                    throw new IllegalStateException();
            }
        } else if (countCache.getAbsolute(getFullSequence(sequence, history)) == 0) {
            NGram backoffHistory = history.backoff(probMode);

            double betaVal =
                    beta.probability(sequence, backoffHistory, recDepth);
            double gammaVal = gamma(sequence, history, recDepth);
            logDebug(recDepth, "beta = {}", betaVal);
            logDebug(recDepth, "gamma = {}", gammaVal);
            return gammaVal * betaVal;
        } else {
            double alphaVal = alpha.probability(sequence, history, recDepth);
            logDebug(recDepth, "alpha = {}", alphaVal);
            return alphaVal;
        }
    }

    /**
     * Wrapper around {@link #calcGamma(NGram, NGram, int)} to add logging and
     * caching.
     */
    public double gamma(NGram sequence, NGram history, int recDepth) {
        logDebug(recDepth, "gamma({},{})", sequence, history);
        ++recDepth;

        Double result = gammaCache.get(history);
        if (result != null) {
            logDebug(recDepth, "result = {} was cached.", result);
            return result;
        } else {
            result = calcGamma(sequence, history, recDepth);
            gammaCache.put(history, result);
            logDebug(recDepth, "result = {}", result);
            return result;
        }
    }

    public double calcGamma(NGram sequence, NGram history, int recDepth) {
        double sumAlpha = 0;
        double sumBeta = 0;

        NGram backoffHistory = history.backoff(probMode);

        for (String word : countCache.getWords()) {
            NGram s = history.concat(word);
            if (countCache.getAbsolute(s) == 0) {
                sumBeta +=
                        beta.probability(new NGram(word), backoffHistory,
                                recDepth);
            } else {
                sumAlpha +=
                        alpha.probability(new NGram(word), history, recDepth);
            }
        }

        logDebug(recDepth, "sumAlpha = {}", sumAlpha);
        logDebug(recDepth, "sumBeta = {}", sumBeta);

        if (sumBeta == 0) {
            return 0.0;
        } else {
            return (1.0 - sumAlpha) / sumBeta;
        }
    }

}
