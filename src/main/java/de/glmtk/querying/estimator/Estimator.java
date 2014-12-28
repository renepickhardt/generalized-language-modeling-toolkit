package de.glmtk.querying.estimator;

import static de.glmtk.common.PatternElem.SKP_WORD;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.common.CountCache;
import de.glmtk.common.NGram;
import de.glmtk.common.ProbMode;
import de.glmtk.querying.estimator.substitute.SubstituteEstimator;
import de.glmtk.util.StringUtils;

public abstract class Estimator {

    private static final Logger LOGGER = LogManager.getLogger(Estimator.class);

    protected final SubstituteEstimator SUBSTITUTE_ESTIMATOR =
            Estimators.ABS_UNIGRAM;

    protected CountCache countCache;

    protected ProbMode probMode;

    public void setCountCache(CountCache countCache) {
        this.countCache = countCache;

        if (SUBSTITUTE_ESTIMATOR != null && SUBSTITUTE_ESTIMATOR != this) {
            SUBSTITUTE_ESTIMATOR.setCountCache(countCache);
        }
    }

    public void setProbMode(ProbMode probMode) {
        this.probMode = probMode;

        if (SUBSTITUTE_ESTIMATOR != null && SUBSTITUTE_ESTIMATOR != this) {
            SUBSTITUTE_ESTIMATOR.setProbMode(probMode);
        }
    }

    /**
     * Wrapper around {@link #probability(NGram, NGram, int)} to hide recDepth
     * parameter, and to perform error checking.
     */
    public final double probability(NGram sequence, NGram history) {
        if (countCache == null) {
            throw new NullPointerException(
                    "You have to set a countCache that is not null before using this method");
        }
        if (probMode == null) {
            throw new NullPointerException(
                    "You have to set a probability mode that is not null before using this method.");
        }

        return probability(sequence, history, 1);
    }

    /**
     * This method should only be called from other estimators. All other
     * users probably want to call {@link #probability(NGram, NGram)}.
     *
     * Wrapper around {@link #calcProbability(NGram, NGram, int)} to add
     * logging.
     */
    public final double
        probability(NGram sequence, NGram history, int recDepth) {
        logDebug(recDepth, "{}#probability({},{})", getClass().getSimpleName(),
                sequence, history);
        ++recDepth;

        double result = calcProbability(sequence, history, recDepth);
        logDebug(recDepth, "result = {}", result);

        return result;
    }

    protected abstract double calcProbability(
            NGram sequence,
            NGram history,
            int recDepth);

    protected static final NGram getFullSequence(NGram sequence, NGram history) {
        return history.concat(sequence);
    }

    protected static final NGram getFullHistory(NGram sequence, NGram history) {
        List<String> skippedSequence = new ArrayList<String>(sequence.size());
        for (int i = 0; i != sequence.size(); ++i) {
            skippedSequence.add(SKP_WORD);
        }
        return history.concat(new NGram(skippedSequence));
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
