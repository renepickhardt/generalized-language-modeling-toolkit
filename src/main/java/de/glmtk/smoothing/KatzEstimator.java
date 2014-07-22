package de.glmtk.smoothing;

import java.util.List;

import de.glmtk.utils.StringUtils;

public class KatzEstimator extends Estimator {

    @Override
    protected double propabilityCond(
            List<String> reqSequence,
            List<String> condSequence,
            int recDepth) {
        debugPropabilityCond(reqSequence, condSequence, recDepth);
        ++recDepth;

        List<String> sequence = getSequence(reqSequence, condSequence);
        int sequenceCount = corpus.getAbsolute(sequence);
        debugSequence(sequence, sequenceCount, recDepth);

        List<String> history = getHistory(reqSequence, condSequence);
        int historyCount;
        if (history.isEmpty()) {
            historyCount = corpus.getNumWords();
        } else {
            historyCount = corpus.getAbsolute(history);
        }
        debugHistory(history, historyCount, recDepth);

        double result;
        if (sequenceCount != 0) {
            result =
                    discountCoefficient(sequenceCount, recDepth)
                            * sequenceCount / historyCount;
        } else {
            result = 0;
        }

        logger.debug(StringUtils.repeat("  ", recDepth) + "result = " + result);

        return result;
    }

    protected double turingEstimate(int r, int recDepth) {
        ++recDepth;
        logger.debug(StringUtils.repeat("  ", recDepth) + "turingDiscount(" + r
                + ")");
        double n_r = corpus.getNGramTimesCount(Corpus.CNT_PATTERN, r);
        double n_rp1 = corpus.getNGramTimesCount(Corpus.CNT_PATTERN, r + 1);
        double result = (r + 1) * n_rp1 / n_r;
        logger.debug(StringUtils.repeat("  ", recDepth) + "n_r = " + n_r);
        logger.debug(StringUtils.repeat("  ", recDepth) + "n_rp1 = " + n_rp1);
        logger.debug(StringUtils.repeat("  ", recDepth) + "result = " + result);
        return result;
    }

    protected double discountCoefficient(int r, int recDepth) {
        ++recDepth;
        logger.debug(StringUtils.repeat("  ", recDepth)
                + "discountCoefficient(" + r + ")");
        double result = turingEstimate(r, recDepth) / r;
        logger.debug(StringUtils.repeat("  ", recDepth) + "result = " + result);
        return result;
    }

}
