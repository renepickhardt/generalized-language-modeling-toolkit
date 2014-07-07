package de.typology.smoothing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.typology.utils.StringUtils;

public class BackoffEstimator extends Estimator {

    private Estimator alpha;

    private Estimator beta;

    private Map<List<String>, Double> gammaCache =
            new HashMap<List<String>, Double>();

    public BackoffEstimator(
            Corpus corpus,
            Estimator alpha) {
        super(corpus);
        this.alpha = alpha;
        beta = this;
    }

    public BackoffEstimator(
            Corpus corpus,
            Estimator alpha,
            Estimator beta) {
        super(corpus);
        this.alpha = alpha;
        this.beta = beta;
    }

    @Override
    public double propabilityCond(
            List<String> reqSequence,
            List<String> condSequence,
            int recDepth) {
        debugPropabilityCond(reqSequence, condSequence, recDepth);
        ++recDepth;

        List<String> sequence = new ArrayList<String>();
        sequence.addAll(condSequence);
        sequence.addAll(reqSequence);

        if (corpus.getAbsolute(sequence) == 0) {
            if (condSequence.isEmpty()) {
                // TODO: Rene: marker for double check
                return 0;
            }

            List<String> condSequence2 =
                    condSequence.subList(1, condSequence.size());

            double betaVal =
                    beta.propabilityCond(reqSequence, condSequence2, recDepth);
            double gammaVal = calcGamma(condSequence, recDepth);
            double result = gammaVal * betaVal;
            logger.debug(StringUtils.repeat("  ", recDepth) + "gamma("
                    + condSequence + ") = " + gammaVal);
            logger.debug(StringUtils.repeat("  ", recDepth) + "beta("
                    + reqSequence + ", " + condSequence2 + ") = " + betaVal);
            logger.debug(StringUtils.repeat("  ", recDepth)
                    + "returning gamma * beta = " + result);
            return result;
        } else {
            double alphaVal =
                    alpha.propabilityCond(reqSequence, condSequence, recDepth);
            logger.debug(StringUtils.repeat("  ", recDepth)
                    + "returning alpha(" + reqSequence + ", " + condSequence
                    + ") = " + alphaVal);
            return alphaVal;
        }
    }

    public double calcGamma(List<String> condSequence, int recDepth) {
        logger.debug(StringUtils.repeat("  ", recDepth) + "calcGamma("
                + condSequence + ")");
        ++recDepth;

        Double result = gammaCache.get(condSequence);
        if (result != null) {
            logger.debug(StringUtils.repeat("  ", recDepth)
                    + "returning cached = " + result);
            return result;
        }

        double sumAlpha = 0;
        double sumBeta = 0;

        List<String> condSequence2 =
                condSequence.subList(1, condSequence.size());

        for (String word : corpus.getWords()) {
            List<String> sequence = new ArrayList<String>();
            sequence.addAll(condSequence);
            sequence.add(word);

            if (corpus.getAbsolute(sequence) == 0) {
                sumBeta +=
                        beta.propabilityCond(Arrays.asList(word),
                                condSequence2, recDepth);
            } else {
                sumAlpha +=
                        alpha.propabilityCond(Arrays.asList(word),
                                condSequence, recDepth);
            }
        }

        logger.debug(StringUtils.repeat("  ", recDepth) + "sumAlpha = "
                + sumAlpha);
        logger.debug(StringUtils.repeat("  ", recDepth) + "sumBeta = "
                + sumBeta);

        if (sumBeta == 0) {
            // TODO: Rene: marker for double check
            result = 0.;
        } else {
            result = (1 - sumAlpha) / sumBeta;
        }

        gammaCache.put(condSequence, result);

        logger.debug(StringUtils.repeat("  ", recDepth)
                + "returning (1 - sumAlpha) / sumBeta = " + result);
        return result;
    }
}
