package de.glmtk.smoothing;

import java.util.List;

import de.glmtk.utils.StringUtils;

public abstract class FractionEstimator extends Estimator {

    /**
     * If you changes this, double check
     * {@link DiscountEstimator#propabilityCond(List, List, int)}
     */
    @Override
    protected double propabilityCond(
            List<String> reqSequence,
            List<String> condSequence,
            int recDepth) {
        debugPropabilityCond(reqSequence, condSequence, recDepth);
        ++recDepth;

        double result;
        // TODO: check if works with continuation counter mle
        if (!condSequence.isEmpty() && corpus.getAbsolute(condSequence) == 0) {
            // Pcond(reqSequence | condSequence) is not well defined.
            logger.debug(StringUtils.repeat("  ", recDepth)
                    + "condSequenceCount = 0");
            result = substitutePropability(reqSequence, recDepth);
        } else {
            double denominator =
                    getDenominator(reqSequence, condSequence, recDepth);
            // TODO: Rene: check if this is formally correct
            if (denominator == 0) {
                logger.debug(StringUtils.repeat("  ", recDepth)
                        + "denominator = 0");
                result = substitutePropability(reqSequence, recDepth);
            } else {
                double numerator =
                        getNumerator(reqSequence, condSequence, recDepth);
                result = numerator / denominator;
            }
        }

        logger.debug(StringUtils.repeat("  ", recDepth) + "result = " + result);
        return result;
    }

    protected abstract double getNumerator(
            List<String> reqSequence,
            List<String> condSequence,
            int recDepth);

    protected abstract double getDenominator(
            List<String> reqSequence,
            List<String> condSequence,
            int recDepth);

}
