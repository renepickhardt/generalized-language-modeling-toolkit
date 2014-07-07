package de.typology.smoothing;

import java.util.ArrayList;
import java.util.List;

import de.typology.patterns.PatternElem;
import de.typology.utils.StringUtils;

public abstract class FractionEstimator extends Estimator {

    public enum BackoffCalc {
        UNIGRAM_ABSOLUTE,

        UNIGRAM_CONTINUATION,

        UNIFORM
    }

    protected BackoffCalc backoffCalc;

    protected static List<String> doubleSkippedList;
    static {
        doubleSkippedList = new ArrayList<String>();
        doubleSkippedList.add(PatternElem.SKIPPED_WORD);
        doubleSkippedList.add(PatternElem.SKIPPED_WORD);
    }

    public FractionEstimator(
            Corpus corpus) {
        super(corpus);
        backoffCalc = BackoffCalc.UNIGRAM_ABSOLUTE;
    }

    @Override
    public double propabilityCond(
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

    protected double substitutePropability(List<String> reqSequence, int recDepth) {
        switch (backoffCalc) {
            case UNIGRAM_ABSOLUTE:
                logger.debug(StringUtils.repeat("  ", recDepth)
                        + "returning unigram distribution (absolute)");
                return (double) corpus.getAbsolute(reqSequence.subList(0, 1))
                        / corpus.getNumWords();

            case UNIGRAM_CONTINUATION:
                logger.debug(StringUtils.repeat("  ", recDepth)
                        + "returning unigram distribution (continuation)");
                reqSequence.add(0, PatternElem.SKIPPED_WORD);
                return (double) corpus.getContinuation(
                        reqSequence.subList(0, 1)).getOnePlusCount()
                        / corpus.getVocabSize() / corpus.getVocabSize();
                // TODO: Rene: why is this wrong
                //return (double) corpus.getContinuation(
                //        reqSequence.subList(0, 1)).getOnePlusCount()
                //        / corpus.getContinuation(doubleSkippedList)
                //                .getOnePlusCount();

            default:
            case UNIFORM:
                logger.debug(StringUtils.repeat("  ", recDepth)
                        + "returning uniform distribution (1/vocabSize)");
                return 1.0 / corpus.getVocabSize();
        }
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
