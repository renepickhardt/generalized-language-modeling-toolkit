package de.typology.smoothing;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.typology.patterns.PatternElem;
import de.typology.utils.StringUtils;

public abstract class Estimator {

    protected static Logger logger = LogManager.getLogger(Estimator.class);

    protected Corpus corpus = null;

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
