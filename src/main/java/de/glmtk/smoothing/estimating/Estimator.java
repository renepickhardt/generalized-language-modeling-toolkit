package de.glmtk.smoothing.estimating;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.patterns.PatternElem;
import de.glmtk.smoothing.Corpus;
import de.glmtk.smoothing.NGram;
import de.glmtk.utils.StringUtils;

public abstract class Estimator {

    /**
     * Each subclass should overwrite this, in order to display the correct
     * class while logging.
     */
    private static final Logger LOGGER = LogManager.getLogger(Estimator.class);

    protected static final Estimator SUBSTITUTE_ESTIMATOR =
            Estimators.ABSOLUTE_UNIGRAM_ESTIMATOR;

    protected Corpus corpus = null;

    public void setCorpus(Corpus corpus) {
        this.corpus = corpus;

        if (SUBSTITUTE_ESTIMATOR != null && SUBSTITUTE_ESTIMATOR != this) {
            SUBSTITUTE_ESTIMATOR.setCorpus(corpus);
        }
    }

    /**
     * {@code P(s|h)}
     */
    public final double propability(NGram sequence, NGram history) {
        return propability(sequence, history, 1);
    }

    protected final double propability(
            NGram sequence,
            NGram history,
            int recDepth) {
        logDebug(recDepth, "{}#propability({},{})", getClass().getSimpleName(),
                sequence, history);
        ++recDepth;

        double result = calcPropability(sequence, history, recDepth);
        logDebug(recDepth, "result = {}", result);

        return result;
    }

    protected abstract double calcPropability(
            NGram sequence,
            NGram history,
            int recDepth);

    /**
     * Backoffs {@code sequence} until absolute count of it is greater zero. If
     * not possible returns zero. Returned sequence may be empty.
     */
    protected final NGram backoffUntilSeen(NGram sequence) {
        while (true) {
            if (sequence.isEmpty() || corpus.getAbsolute(sequence) != 0) {
                return sequence;
            }
            sequence = sequence.backoff();
        }
    }

    /**
     * {@code fullSequence = history + sequence}
     */
    protected static final NGram getFullSequence(NGram sequence, NGram history) {
        return history.concat(sequence);
    }

    /**
     * {@code fullHistory = history + SKPs (num sequence.size())}
     */
    protected static final NGram getFullHistory(NGram sequence, NGram history) {
        NGram fullHistory = history;
        for (int i = 0; i != sequence.size(); ++i) {
            fullHistory = fullHistory.concat(PatternElem.SKIPPED_WORD);
        }
        return fullHistory;
    }

    protected static final void logDebug(int recDepth, String message) {
        LOGGER.debug("{}" + message, StringUtils.repeat("  ", recDepth));
    }

    protected static final void logDebug(
            int recDepth,
            String message,
            Object... params) {
        Object[] logParams = new Object[params.length + 1];
        logParams[0] = StringUtils.repeat("  ", recDepth);
        System.arraycopy(params, 0, logParams, 1, params.length);

        LOGGER.debug("{}" + message, logParams);
    }

}
