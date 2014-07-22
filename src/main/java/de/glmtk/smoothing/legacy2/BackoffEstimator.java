package de.glmtk.smoothing.legacy2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.glmtk.smoothing.Corpus;
import de.glmtk.utils.StringUtils;

public class BackoffEstimator extends Estimator {

    private static Map<Corpus, Map<List<String>, Double>> globalGammaCache =
            new HashMap<Corpus, Map<List<String>, Double>>();

    private Map<List<String>, Double> gammaCache;

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
        if (alpha != this) {
            alpha.setCorpus(corpus);
        }
        if (beta != this) {
            beta.setCorpus(corpus);
        }

        gammaCache = globalGammaCache.get(corpus);
        if (gammaCache == null) {
            gammaCache = new HashMap<List<String>, Double>();
            globalGammaCache.put(corpus, gammaCache);
        }
    }

    @Override
    protected double propabilityCond(
            List<String> reqSequence,
            List<String> condSequence,
            int recDepth) {
        debugPropabilityCond(reqSequence, condSequence, recDepth);
        ++recDepth;

        List<String> sequence = getSequence(reqSequence, condSequence);

        if (corpus.getAbsolute(sequence) == 0) {
            if (condSequence.isEmpty()) {
                // TODO: Rene: marker for double check
                return 0;
            }

            List<String> condSequence2 =
                    condSequence.subList(1, condSequence.size());

            double betaVal =
                    beta.propabilityCond(reqSequence, condSequence2, recDepth);
            double gammaVal = gamma(condSequence, recDepth);
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

    protected double gamma(List<String> condSequence, int recDepth) {
        logger.debug(StringUtils.repeat("  ", recDepth) + "gamma("
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
            result = 0.0;
        } else {
            result = (1 - sumAlpha) / sumBeta;
        }

        gammaCache.put(condSequence, result);

        logger.debug(StringUtils.repeat("  ", recDepth)
                + "returning (1 - sumAlpha) / sumBeta = " + result);
        return result;
    }
}
