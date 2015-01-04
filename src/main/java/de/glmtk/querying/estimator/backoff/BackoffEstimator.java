package de.glmtk.querying.estimator.backoff;

import java.util.HashMap;
import java.util.Map;

import de.glmtk.common.BackoffMode;
import de.glmtk.common.CountCache;
import de.glmtk.common.NGram;
import de.glmtk.common.ProbMode;
import de.glmtk.querying.estimator.Estimator;

public class BackoffEstimator extends Estimator {
    private Map<NGram, Double> gammaCache;
    private Estimator alpha;
    private Estimator beta;

    public BackoffEstimator(Estimator alpha) {
        this.alpha = alpha;
        beta = this;
    }

    public BackoffEstimator(Estimator alpha,
                            Estimator beta) {
        this.alpha = alpha;
        this.beta = beta;
    }

    @Override
    public void setCountCache(CountCache countCache) {
        super.setCountCache(countCache);
        alpha.setCountCache(countCache);
        if (beta != this)
            beta.setCountCache(countCache);

        gammaCache = new HashMap<NGram, Double>();
    }

    @Override
    public void setProbMode(ProbMode probMode) {
        super.setProbMode(probMode);
        alpha.setProbMode(probMode);
        if (beta != this)
            beta.setProbMode(probMode);

        gammaCache = new HashMap<NGram, Double>();
    }

    @Override
    protected double calcProbability(NGram sequence,
                                     NGram history,
                                     int recDepth) {
        if (history.isEmpty())
            switch (probMode) {
                case COND:
                    return 0;
                case MARG:
                    return SUBSTITUTE_ESTIMATOR.probability(sequence, history,
                            recDepth);
                default:
                    throw new IllegalStateException();
            }
        else if (countCache.getAbsolute(getFullSequence(sequence, history)) == 0) {
            NGram backoffHistory = history.backoff(BackoffMode.DEL); // TODO: fix backoff arg

            double betaVal = beta.probability(sequence, backoffHistory,
                    recDepth);
            double gammaVal = gamma(sequence, history, recDepth);
            logTrace(recDepth, "beta = %f", betaVal);
            logTrace(recDepth, "gamma = %f", gammaVal);
            return gammaVal * betaVal;
        } else {
            double alphaVal = alpha.probability(sequence, history, recDepth);
            logTrace(recDepth, "alpha = %f", alphaVal);
            return alphaVal;
        }
    }

    /**
     * Wrapper around {@link #calcGamma(NGram, NGram, int)} to add logging and
     * caching.
     */
    public double gamma(NGram sequence,
                        NGram history,
                        int recDepth) {
        logTrace(recDepth, "gamma(%s,%s)", sequence, history);
        ++recDepth;

        Double result = gammaCache.get(history);
        if (result != null) {
            logTrace(recDepth, "result = %f was cached.", result);
            return result;
        } else {
            result = calcGamma(sequence, history, recDepth);
            gammaCache.put(history, result);
            logTrace(recDepth, "result = %f", result);
            return result;
        }
    }

    public double calcGamma(NGram sequence,
                            NGram history,
                            int recDepth) {
        double sumAlpha = 0;
        double sumBeta = 0;

        NGram backoffHistory = history.backoff(BackoffMode.DEL); // TODO: fix backoff arg

        for (String word : countCache.getWords()) {
            NGram s = history.concat(word);
            if (countCache.getAbsolute(s) == 0)
                sumBeta += beta.probability(new NGram(word), backoffHistory,
                        recDepth);
            else
                sumAlpha += alpha.probability(new NGram(word), history,
                        recDepth);
        }

        logTrace(recDepth, "sumAlpha = %f", sumAlpha);
        logTrace(recDepth, "sumBeta = %f", sumBeta);

        if (sumBeta == 0)
            return 0.0;
        else
            return (1.0 - sumAlpha) / sumBeta;
    }
}
