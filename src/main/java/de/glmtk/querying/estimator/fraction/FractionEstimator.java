package de.glmtk.querying.estimator.fraction;

import de.glmtk.common.NGram;
import de.glmtk.querying.estimator.Estimator;

public abstract class FractionEstimator extends Estimator {
    @Override
    protected double calcProbability(NGram sequence,
                                     NGram history,
                                     int recDepth) {
        if (history.seen(countCache)) {
            double numeratorVal = numerator(sequence, history, recDepth);
            double denominatorVal = denominator(sequence, history, recDepth);
            if (denominatorVal != 0)
                return numeratorVal / denominatorVal;
        }

        // Fraction estimator probability is undefined.
        switch (probMode) {
            case COND:
                logTrace(recDepth, "Probability undefined, returning 0 (COND):");
                return 0;
            case MARG:
                logTrace(recDepth,
                        "Probability undefined, returning substitute (MARG):");
                return SUBSTITUTE_ESTIMATOR.probability(sequence, history,
                        recDepth);
            default:
                throw new IllegalStateException();
        }
    }

    public final double numerator(NGram sequence,
                                  NGram history,
                                  int recDepth) {
        logTrace(recDepth, "numerator(%s,%s)", sequence, history);
        ++recDepth;

        double result = calcNumerator(sequence, history, recDepth);
        logTrace(recDepth, "result = %f", result);
        return result;
    }

    protected abstract double calcNumerator(NGram sequence,
                                            NGram history,
                                            int recDepth);

    public final double denominator(NGram sequence,
                                    NGram history,
                                    int recDepth) {
        logTrace(recDepth, "denominator(%s,%s)", sequence, history);
        ++recDepth;

        double result = calcDenominator(sequence, history, recDepth);
        logTrace(recDepth, "result = %f", result);
        return result;
    }

    protected abstract double calcDenominator(NGram sequence,
                                              NGram history,
                                              int recDepth);
}
