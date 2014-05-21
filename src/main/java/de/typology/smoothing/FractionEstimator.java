package de.typology.smoothing;

import java.util.List;

public abstract class FractionEstimator extends Estimator {

    public FractionEstimator(
            Corpus corpus) {
        super(corpus);
    }

    @Override
    public double propabilityCond(
            List<String> reqSequence,
            List<String> condSequence) {
        debugPropabilityCond(reqSequence, condSequence);

        double result;
        // TODO: check if works with continuation counter mle
        if (!condSequence.isEmpty() && corpus.getAbsolute(condSequence) == 0) {
            // Pcond(reqSequence | condSequence) is not well defined.
            logger.debug("    condSequenceCount = 0, returning 0");
            result = 0;
        } else {
            double denominator = getDenominator(reqSequence, condSequence);
            // TODO: Rene: check if this is formally correct
            if (denominator == 0) {
                logger.debug("    denominator = 0, returning 0");
                result = 0;
            } else {
                double numerator = getNumerator(reqSequence, condSequence);
                result = numerator / denominator;
            }
        }

        logger.debug("    result = " + result);
        return result;
    }

    protected abstract double getNumerator(
            List<String> reqSequence,
            List<String> condSequence);

    protected abstract double getDenominator(
            List<String> reqSequence,
            List<String> condSequence);

}
