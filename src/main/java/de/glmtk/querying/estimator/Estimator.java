/*
 * Generalized Language Modeling Toolkit (GLMTK)
 *
 * Copyright (C) 2014-2015 Lukas Schmelzeisen, Rene Pickhardt
 *
 * GLMTK is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * GLMTK is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * GLMTK. If not, see <http://www.gnu.org/licenses/>.
 *
 * See the AUTHORS file for contributors.
 */

package de.glmtk.querying.estimator;

import static de.glmtk.common.PatternElem.SKP_WORD;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.common.CountCache;
import de.glmtk.common.NGram;
import de.glmtk.common.ProbMode;
import de.glmtk.querying.estimator.substitute.SubstituteEstimator;
import de.glmtk.util.StringUtils;

public abstract class Estimator {
    private static final Logger LOGGER = LogManager.getFormatterLogger(Estimator.class);

    protected static final NGram getFullSequence(NGram sequence,
                                                 NGram history) {
        return history.concat(sequence);
    }

    protected static final NGram getFullHistory(NGram sequence,
                                                NGram history) {
        List<String> skippedSequence = new ArrayList<>(sequence.size());
        for (int i = 0; i != sequence.size(); ++i)
            skippedSequence.add(SKP_WORD);
        return history.concat(new NGram(skippedSequence));
    }

    protected static final void logTrace(int recDepth,
                                         String message) {
        LOGGER.trace(StringUtils.repeat("  ", recDepth) + message);
    }

    protected static final void logTrace(int recDepth,
                                         String format,
                                         Object... params) {
        LOGGER.trace(StringUtils.repeat("  ", recDepth) + format, params);
    }

    protected final SubstituteEstimator SUBSTITUTE_ESTIMATOR = Estimators.ABS_UNIGRAM;
    private String name = "Unnamed";
    protected CountCache countCache = null;
    protected ProbMode probMode = null;

    @Override
    public String toString() {
        return name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCountCache(CountCache countCache) {
        this.countCache = countCache;

        if (SUBSTITUTE_ESTIMATOR != null && SUBSTITUTE_ESTIMATOR != this)
            SUBSTITUTE_ESTIMATOR.setCountCache(countCache);
    }

    public void setProbMode(ProbMode probMode) {
        this.probMode = probMode;

        if (SUBSTITUTE_ESTIMATOR != null && SUBSTITUTE_ESTIMATOR != this)
            SUBSTITUTE_ESTIMATOR.setProbMode(probMode);
    }

    /**
     * Wrapper around {@link #probability(NGram, NGram, int)} to hide recDepth
     * parameter, and to perform error checking.
     */
    public final double probability(NGram sequence,
                                    NGram history) {
        Objects.requireNonNull(countCache,
                "You have to set a countCache that is not null before using this method");
        Objects.requireNonNull(probMode,
                "You have to set a probability mode that is not null before using this method.");

        return probability(sequence, history, 1);
    }

    /**
     * This method should only be called from other estimators. All other users
     * probably want to call {@link #probability(NGram, NGram)}.
     *
     * Wrapper around {@link #calcProbability(NGram, NGram, int)} to add
     * logging.
     */
    public final double probability(NGram sequence,
                                    NGram history,
                                    int recDepth) {
        logTrace(recDepth, "%s#probability(%s,%s)", getClass().getSimpleName(),
                sequence, history);
        ++recDepth;

        double result = calcProbability(sequence, history, recDepth);
        logTrace(recDepth, "result = %f", result);

        return result;
    }

    protected abstract double calcProbability(NGram sequence,
                                              NGram history,
                                              int recDepth);
}
