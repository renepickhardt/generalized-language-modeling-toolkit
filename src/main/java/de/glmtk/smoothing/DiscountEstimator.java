package de.glmtk.smoothing;

import java.util.List;

import de.glmtk.utils.StringUtils;

/**
 * DiscountEstimator does not return a probability distribution, but rather it
 * has to be used in a BackoffEstimator.
 */
public abstract class DiscountEstimator extends Estimator {

    public FractionEstimator fractionEstimator;

    public DiscountEstimator(
            FractionEstimator fractionEstimator) {
        this.fractionEstimator = fractionEstimator;
    }

    @Override
    public void setCorpus(Corpus corpus) {
        super.setCorpus(corpus);
        fractionEstimator.setCorpus(corpus);
    }

    /**
     * Implementation similar to
     * {@link FractionEstimator#propabilityCond(List, List, int)}
     */
    @Override
    protected double propabilityCond(
            List<String> reqSequence,
            List<String> condSequence,
            int recDepth) {
        debugPropabilityCond(reqSequence, condSequence, recDepth);
        ++recDepth;

        double result;
        if (!condSequence.isEmpty() && corpus.getAbsolute(condSequence) == 0) {
            // Pcond(reqSequence | condSequence) is not well defined.
            logger.debug(StringUtils.repeat("  ", recDepth)
                    + "condSequenceCount = 0");
            result = substitutePropability(reqSequence, recDepth);
        } else {
            double denominator =
                    fractionEstimator.getDenominator(reqSequence, condSequence,
                            recDepth);
            if (denominator == 0) {
                logger.debug(StringUtils.repeat("  ", recDepth)
                        + "denominator = 0");
                result = substitutePropability(reqSequence, recDepth);
            } else {
                double numerator =
                        fractionEstimator.getNumerator(reqSequence,
                                condSequence, recDepth);
                result =
                        (numerator - discount(reqSequence, condSequence,
                                recDepth)) / denominator;
            }
        }

        logger.debug(StringUtils.repeat("  ", recDepth) + "result = " + result);
        return result;
    }

    protected abstract double discount(
            List<String> reqSequence,
            List<String> condSequence,
            int recDepth);

}
