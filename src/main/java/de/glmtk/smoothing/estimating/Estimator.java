package de.glmtk.smoothing.estimating;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.patterns.PatternElem;
import de.glmtk.smoothing.CalculatingMode;
import de.glmtk.smoothing.Corpus;
import de.glmtk.smoothing.NGram;
import de.glmtk.utils.StringUtils;

/**
 * {@code P(s | h)}
 */
public abstract class Estimator {

    /**
     * Each subclass should overwrite this, in order to display the correct
     * class while logging.
     */
    private static final Logger LOGGER = LogManager.getLogger(Estimator.class);

    protected final Estimator SUBSTITUTE_ESTIMATOR =
            Estimators.ABSOLUTE_UNIGRAM;

    protected Corpus corpus = null;

    public void setCorpus(Corpus corpus) {
        this.corpus = corpus;

        if (SUBSTITUTE_ESTIMATOR != null && SUBSTITUTE_ESTIMATOR != this) {
            SUBSTITUTE_ESTIMATOR.setCorpus(corpus);
        }
    }

    /**
     * Easy public api for {@link #probability(NGram, NGram, int)}.
     */
    public final double probability(
            NGram sequence,
            NGram history,
            CalculatingMode calculatingMode) {
        return probability(sequence, history, calculatingMode, 1);
    }

    /**
     * Wrapper around
     * {@link #calcProbability(NGram, NGram, CalculatingMode, int)} to add
     * logging.
     */
    protected final double probability(
            NGram sequence,
            NGram history,
            CalculatingMode calculatingMode,
            int recDepth) {
        logDebug(recDepth, "{}#propability({},{})", getClass().getSimpleName(),
                sequence, history);
        ++recDepth;

        double result =
                calcProbability(sequence, history, calculatingMode, recDepth);
        logDebug(recDepth, "result = {}", result);

        return result;
    }

    protected abstract double calcProbability(
            NGram sequence,
            NGram history,
            CalculatingMode calculatingMode,
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
     * {@code fs = h . s}
     */
    protected static final NGram getFullSequence(NGram sequence, NGram history) {
        return history.concat(sequence);
    }

    /**
     * {@code fh = h . SKPs (#s)}
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
