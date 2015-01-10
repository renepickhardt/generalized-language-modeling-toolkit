package de.glmtk.querying.estimator.fast;

import static de.glmtk.common.NGram.SKP_NGRAM;
import static de.glmtk.common.NGram.WSKP_NGRAM;
import de.glmtk.common.Counter;
import de.glmtk.common.NGram;

public class FastModifiedKneserNeyEstimator extends FastModifiedKneserNeyAbsEstimator {
    @Override
    protected double calcProbability(NGram sequence,
                                     NGram history,
                                     int recDepth) {
        double denominator = countCache.getAbsolute(history.concat(SKP_NGRAM));

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
                Counter c = countCache.getContinuation(history.concat(WSKP_NGRAM));
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
        double beta = probabilityLower(sequence, backoffHistory, recDepth);

        return alpha + gamma * beta;
    }

    public final double probabilityLower(NGram sequence,
                                         NGram history,
                                         int recDepth) {
        logTrace(recDepth, "%s#probabilityLower(%s,%s)",
                getClass().getSimpleName(), sequence, history);
        ++recDepth;

        double result = calcProbabilityLower(sequence, history, recDepth);
        logTrace(recDepth, "result = %f", result);

        return result;
    }

    protected double calcProbabilityLower(NGram sequence,
                                          NGram history,
                                          int recDepth) {
        double denominator = countCache.getContinuation(
                WSKP_NGRAM.concat(history.concat(SKP_NGRAM).convertSkpToWskp())).getOnePlusCount();

        if (history.isEmptyOrOnlySkips()) {
            if (denominator == 0.0)
                return (double) countCache.getAbsolute(sequence.get(0))
                        / countCache.getNumWords();

            double numerator = countCache.getContinuation(
                    WSKP_NGRAM.concat(history.concat(sequence)).convertSkpToWskp()).getOnePlusCount();
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
                Counter c = countCache.getContinuation(history.concat(WSKP_NGRAM));
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
            double numerator = countCache.getContinuation(
                    WSKP_NGRAM.concat(history.concat(sequence).convertSkpToWskp())).getOnePlusCount();
            if (numerator > discount)
                numerator -= discount;
            else
                numerator = 0.0;

            alpha = numerator / denominator;
        }

        NGram backoffHistory = history.backoffUntilSeen(backoffMode, countCache);
        double beta = probabilityLower(sequence, backoffHistory, recDepth);

        return alpha + gamma * beta;
    }
}
