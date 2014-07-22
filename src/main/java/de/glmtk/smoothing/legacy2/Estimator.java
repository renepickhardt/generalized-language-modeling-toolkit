package de.glmtk.smoothing.legacy2;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.patterns.PatternElem;
import de.glmtk.smoothing.Corpus;
import de.glmtk.utils.StringUtils;

public abstract class Estimator {

    protected static Logger logger = LogManager.getLogger(Estimator.class);

    protected static List<String> doubleSkippedList;
    static {
        doubleSkippedList = new ArrayList<String>();
        doubleSkippedList.add(PatternElem.SKIPPED_WORD);
        doubleSkippedList.add(PatternElem.SKIPPED_WORD);
    }

    protected Corpus corpus = null;

    public enum SubstituteCalc {
        UNIGRAM_ABSOLUTE,

        UNIGRAM_CONTINUATION,

        UNIFORM
    }

    protected SubstituteCalc substituteCalc = SubstituteCalc.UNIGRAM_ABSOLUTE;

    public void setCorpus(Corpus corpus) {
        this.corpus = corpus;
    }

    public final double propabilityCond(
            List<String> reqSequence,
            List<String> condSequence) {
        return propabilityCond(reqSequence, condSequence, 1);
    }

    protected abstract double propabilityCond(
            List<String> reqSequence,
            List<String> condSequence,
            int recDepth);

    /**
     * {@code sequence = condSequence + reqSequence}
     */
    protected List<String> getSequence(
            List<String> reqSequence,
            List<String> condSequence) {
        int n = reqSequence.size() + condSequence.size() - 1;

        List<String> sequence = new ArrayList<String>(n);
        sequence.addAll(condSequence);
        sequence.addAll(reqSequence);

        return sequence;
    }

    /**
     * {@code history = condSequence + skp (reqSequence.size)}
     */
    // TODO: put skps in front
    protected List<String> getHistory(
            List<String> reqSequence,
            List<String> condSequence) {
        int n = reqSequence.size() + condSequence.size() - 1;

        List<String> history = new ArrayList<String>(n);
        history.addAll(condSequence);
        for (int i = 0; i != reqSequence.size(); ++i) {
            history.add(PatternElem.SKIPPED_WORD);
        }

        return history;
    }

    protected double substitutePropability(
            List<String> reqSequence,
            int recDepth) {
        switch (substituteCalc) {
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

    protected void debugPropabilityCond(
            List<String> reqSequence,
            List<String> condSequence,
            int recDepth) {
        logger.debug(StringUtils.repeat("  ", recDepth)
                + getClass().getSimpleName() + "#propabilityCond("
                + reqSequence + ", " + condSequence + ")");
    }

    protected void debugSequence(
            List<String> sequence,
            double sequenceCount,
            int recDepth) {
        logger.debug(StringUtils.repeat("  ", recDepth) + "sequence = "
                + sequence + "(count = " + sequenceCount + ")");
    }

    protected void debugHistory(
            List<String> history,
            double historyCount,
            int recDepth) {
        logger.debug(StringUtils.repeat("  ", recDepth) + "history = "
                + history + "(count = " + historyCount + ")");
    }

}
