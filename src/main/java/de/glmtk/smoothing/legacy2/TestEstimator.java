package de.glmtk.smoothing.legacy2;

import java.util.LinkedList;
import java.util.List;

import de.glmtk.patterns.PatternElem;
import de.glmtk.smoothing.Corpus;
import de.glmtk.utils.StringUtils;

public class TestEstimator extends Estimator {

    private double discount;

    private Estimator alpha;

    private Estimator beta;

    public TestEstimator(
            double discount,
            Estimator beta) {
        this.discount = discount;
        alpha =
                new AbsoluteDiscountEstimator(new MaximumLikelihoodEstimator(),
                        discount);
        this.beta = beta;
    }

    @Override
    public void setCorpus(Corpus corpus) {
        super.setCorpus(corpus);
        alpha.setCorpus(corpus);
        beta.setCorpus(corpus);
    }

    @Override
    protected double propabilityCond(
            List<String> reqSequence,
            List<String> condSequence,
            int recDepth) {
        debugPropabilityCond(reqSequence, condSequence, recDepth);
        ++recDepth;

        double result;
        // TODO: what is better entropy? substitute or backoff?
        while (condSequence.isEmpty() || corpus.getAbsolute(condSequence) == 0) {
            if (condSequence.isEmpty()) {
                break;
            }

            condSequence = condSequence.subList(1, condSequence.size());
        }

        if (condSequence.isEmpty()) {
            logger.debug(StringUtils.repeat("  ", recDepth)
                    + "returning substitute propability");
            result = substitutePropability(reqSequence, recDepth);
        } else {
            logger.debug(StringUtils.repeat("  ", recDepth)
                    + "calculating alpha:");
            double alphaVal =
                    alpha.propabilityCond(reqSequence, condSequence,
                            recDepth + 1);
            double gammaVal = gamma(discount, condSequence, recDepth);
            List<String> condSequence2 =
                    condSequence.subList(1, condSequence.size());
            logger.debug(StringUtils.repeat("  ", recDepth)
                    + "calculating beta:");
            double betaVal =
                    beta.propabilityCond(reqSequence, condSequence2,
                            recDepth + 1);

            result = alphaVal + gammaVal * betaVal;
            logger.debug(StringUtils.repeat("  ", recDepth) + "alpha = "
                    + alphaVal);
            logger.debug(StringUtils.repeat("  ", recDepth) + "beta = "
                    + betaVal);
            logger.debug(StringUtils.repeat("  ", recDepth) + "gamma = "
                    + gammaVal);

            logger.debug(StringUtils.repeat("  ", recDepth) + "result = "
                    + result);
        }

        return result;
    }

    private double gamma(
            double discount,
            List<String> condSequence,
            int recDepth) {
        logger.debug(StringUtils.repeat("  ", recDepth) + "caulculating gamma("
                + discount + ", " + condSequence + ")");
        ++recDepth;

        List<String> condSequenceSkp = new LinkedList<String>();
        condSequenceSkp.addAll(condSequence);
        condSequenceSkp.add(PatternElem.SKIPPED_WORD);
        double n_1p = corpus.getContinuation(condSequenceSkp).getOnePlusCount();
        double abs = corpus.getAbsolute(condSequenceSkp);
        double result = discount * n_1p / abs;

        logger.debug(StringUtils.repeat("  ", recDepth) + "n_1p = " + n_1p);
        logger.debug(StringUtils.repeat("  ", recDepth) + "abs = " + abs);
        logger.debug(StringUtils.repeat("  ", recDepth) + "result = " + result);

        return result;
    }
}
