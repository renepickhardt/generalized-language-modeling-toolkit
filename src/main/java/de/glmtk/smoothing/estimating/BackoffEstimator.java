package de.glmtk.smoothing.estimating;

import java.util.HashMap;
import java.util.Map;

import de.glmtk.smoothing.CalculatingMode;
import de.glmtk.smoothing.Corpus;
import de.glmtk.smoothing.NGram;

/**
 * Tests fail for this. But it's literally the same code as
 * {@link de.glmtk.smoothing.legacy2.BackoffEstimator}. Why does it fail then?
 */
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
    public void setCorpus(Corpus corpus) {
        super.setCorpus(corpus);
        alpha.setCorpus(corpus);
        if (beta != this) {
            beta.setCorpus(corpus);
        }

        gammaCache = new HashMap<NGram, Double>();
    }

    @Override
    public void setCalculatingMode(CalculatingMode calculatingMode) {
        super.setCalculatingMode(calculatingMode);
        alpha.setCalculatingMode(calculatingMode);
        if (beta != this) {
            beta.setCalculatingMode(calculatingMode);
        }

        gammaCache = new HashMap<NGram, Double>();
    }

    @Override
    protected double
        calcProbability(NGram sequence, NGram history, int recDepth) {
        NGram fullSequence = getFullSequence(sequence, history);
        if (corpus.getAbsolute(fullSequence) == 0) {
            if (history.isEmpty()) {
                // TODO: Rene: marker for double check
                logDebug(recDepth, "history empty returning 0...");
                return 0;
                //return SUBSTITUTE_ESTIMATOR.probability(sequence, history,
                //        recDepth);
            }

            NGram backoffHistory = history.backoff();

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
    protected double gamma(NGram sequence, NGram history, int recDepth) {
        logDebug(recDepth, "gamma({},{})", sequence, history);
        ++recDepth;

        Double result = gammaCache.get(history);
        if (result != null) {
            logDebug(recDepth, "result = {} was chached.", result);
            return result;
        } else {
            result = calcGamma(sequence, history, recDepth);
            gammaCache.put(history, result);
            logDebug(recDepth, "result = {}", result);
            return result;
        }
    }

    protected double calcGamma(NGram sequence, NGram history, int recDepth) {
        double sumAlpha = 0;
        double sumBeta = 0;

        for (String word : corpus.getWords()) {
            NGram s = history.concat(word);
            if (corpus.getAbsolute(s) == 0) {
                sumBeta +=
                        beta.probability(new NGram(word), history.backoff(),
                                recDepth);
            } else {
                sumAlpha +=
                        alpha.probability(new NGram(word), history, recDepth);
            }
        }

        logDebug(recDepth, "sumAlpha = {}", sumAlpha);
        logDebug(recDepth, "sumBeta = {}", sumBeta);

        if (sumBeta == 0) {
            // TODO: Rene: double check this
            return 0.0;
        } else {
            return (1.0 - sumAlpha) / sumBeta;
        }
    }

}
