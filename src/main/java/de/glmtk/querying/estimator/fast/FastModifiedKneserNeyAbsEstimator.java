package de.glmtk.querying.estimator.fast;

import java.util.HashMap;
import java.util.Map;

import de.glmtk.common.BackoffMode;
import de.glmtk.common.CountCache;
import de.glmtk.common.Counter;
import de.glmtk.common.NGram;
import de.glmtk.common.Pattern;
import de.glmtk.querying.estimator.Estimator;

public class FastModifiedKneserNeyAbsEstimator extends Estimator {
    protected BackoffMode backoffMode;
    private Map<Pattern, double[]> discounts;

    public FastModifiedKneserNeyAbsEstimator() {
        setBackoffMode(BackoffMode.DEL);
    }

    public void setBackoffMode(BackoffMode backoffMode) {
        if (backoffMode != BackoffMode.DEL && backoffMode != BackoffMode.SKP)
            throw new IllegalArgumentException(
                    "Illegal BackoffMode for this class.");
        this.backoffMode = backoffMode;
    }

    @Override
    public void setCountCache(CountCache countCache) {
        super.setCountCache(countCache);
        discounts = new HashMap<>();
    }

    @Override
    protected double calcProbability(NGram sequence,
                                     NGram history,
                                     int recDepth) {
        double denominator = countCache.getAbsolute(history.concat(NGram.SKP_NGRAM));

        if (history.isEmptyOrOnlySkips()) {
            if (denominator == 0.0)
                return (double) countCache.getAbsolute(sequence.get(0))
                        / countCache.getNumWords();

            double numerator = countCache.getAbsolute(history.concat(sequence));
            return numerator / denominator;
        }

        double discount;
        double gamma = 0.0;
        {
            double d[] = getDiscounts(history.getPattern(), recDepth);
            long abs = countCache.getAbsolute(history);
            if (abs == 0)
                discount = 0.0;
            else if (abs == 1)
                discount = d[0];
            else if (abs == 2)
                discount = d[1];
            else
                discount = d[2];

            if (denominator != 0) {
                Counter c = countCache.getContinuation(history.concat(NGram.WSKP_NGRAM));
                gamma = (d[0] * c.getOneCount() + d[1] * c.getTwoCount() + d[2]
                        * c.getThreePlusCount())
                        / denominator;
            }
        }

        double alpha;
        if (denominator == 0.0)
            alpha = (double) countCache.getAbsolute(sequence.get(0))
            / countCache.getNumWords();
        else {
            double numerator = countCache.getAbsolute(history.concat(sequence));
            if (numerator > discount)
                numerator -= discount;
            else
                numerator = 0.0;

            alpha = numerator / denominator;
        }

        NGram backoffHistory = history.backoffUntilSeen(backoffMode, countCache);
        double beta = probability(sequence, backoffHistory, recDepth);

        return alpha + gamma * beta;
    }

    protected double[] getDiscounts(Pattern pattern,
                                    int recDepth) {
        double[] d = discounts.get(pattern);
        if (d != null)
            return d;

        long[] n = countCache.getNGramTimes(pattern);
        double y = (double) n[0] / (n[0] + n[1]);

        d = new double[3];
        d[0] = 1.0 - 2.0 * y * n[1] / n[0];
        d[1] = 2.0 - 3.0 * y * n[2] / n[1];
        d[2] = 3.0 - 4.0 * y * n[3] / n[2];

        discounts.put(pattern, d);
        return d;
    }

}
