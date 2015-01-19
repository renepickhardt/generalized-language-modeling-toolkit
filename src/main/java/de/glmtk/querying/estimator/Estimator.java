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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

import de.glmtk.common.CountCache;
import de.glmtk.common.NGram;
import de.glmtk.common.Pattern;
import de.glmtk.common.ProbMode;
import de.glmtk.counts.Counts;
import de.glmtk.counts.NGramTimes;
import de.glmtk.logging.Logger;
import de.glmtk.querying.calculator.SequenceCalculator;
import de.glmtk.querying.estimator.substitute.AbsoluteUnigramEstimator;
import de.glmtk.querying.estimator.substitute.SubstituteEstimator;
import de.glmtk.util.StringUtils;

public abstract class Estimator {
    private static final Logger LOGGER = Logger.get(Estimator.class);

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

    protected final SubstituteEstimator SUBSTITUTE_ESTIMATOR;
    private String name;
    protected CountCache countCache;
    protected ProbMode probMode;

    public Estimator() {
        if (this instanceof SubstituteEstimator)
            SUBSTITUTE_ESTIMATOR = null;
        else
            SUBSTITUTE_ESTIMATOR = new AbsoluteUnigramEstimator();
        name = "Unnamed";
        countCache = null;
        probMode = null;
    }

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

    public static class PatternTrackingCountCache extends CountCache {
        private Set<Pattern> usedPatterns = new HashSet<>();
        private Random random = new Random();

        public PatternTrackingCountCache() throws Exception {
            super(null, null);
        }

        public Set<Pattern> getUsedPatterns() {
            return usedPatterns;
        }

        @Override
        public long getAbsolute(NGram sequence) {
            usedPatterns.add(sequence.getPattern());

            // is it possible that sequence is unseen?
            if (sequence.isEmptyOrOnlySkips())
                return random.nextInt(10) + 1;
            return random.nextInt(11);
        }

        @Override
        public Counts getContinuation(NGram sequence) {
            usedPatterns.add(sequence.getPattern());

            // is it possible that sequence is unseen?
            if (sequence.isEmptyOrOnlySkips())
                return new Counts(random.nextInt(10) + 1,
                        random.nextInt(10) + 1, random.nextInt(10) + 1,
                        random.nextInt(10) + 1);

            return new Counts(random.nextInt(11), random.nextInt(11),
                    random.nextInt(11), random.nextInt(11));
        }

        @Override
        public NGramTimes getNGramTimes(Pattern pattern) {
            usedPatterns.add(pattern);
            return new NGramTimes(random.nextInt(10) + 1,
                    random.nextInt(10) + 1, random.nextInt(10) + 1,
                    random.nextInt(10) + 1);
        }
    }

    public Set<Pattern> getUsedPatterns(int modelSize,
                                        ProbMode probMode) throws Exception {
        final Set<Pattern> usedPatterns = new HashSet<>();

        CountCache trackingCountCache = new CountCache(null, null) {
            private Random random = new Random();

            @Override
            public long getAbsolute(NGram sequence) {
                usedPatterns.add(sequence.getPattern());

                // is it possible that sequence is unseen?
                if (sequence.isEmptyOrOnlySkips())
                    return random.nextInt(10) + 1;
                return random.nextInt(11);
            }

            @Override
            public Counts getContinuation(NGram sequence) {
                usedPatterns.add(sequence.getPattern());

                // is it possible that sequence is unseen?
                if (sequence.isEmptyOrOnlySkips())
                    return new Counts(random.nextInt(10) + 1,
                            random.nextInt(10) + 1, random.nextInt(10) + 1,
                            random.nextInt(10) + 1);

                return new Counts(random.nextInt(11), random.nextInt(11),
                        random.nextInt(11), random.nextInt(11));
            }

            @Override
            public NGramTimes getNGramTimes(Pattern pattern) {
                usedPatterns.add(pattern);
                return new NGramTimes(random.nextInt(10) + 1,
                        random.nextInt(10) + 1, random.nextInt(10) + 1,
                        random.nextInt(10) + 1);
            }
        };

        setCountCache(trackingCountCache);

        SequenceCalculator calculator = new SequenceCalculator();
        calculator.setEstimator(this);
        calculator.setProbMode(probMode);

        Logger.setTraceEnabled(false);

        for (int n = 0; n != modelSize; ++n) {
            List<String> sequence = new ArrayList<>(n);
            for (int i = 0; i != n + 1; ++i)
                sequence.add("a");
            for (int i = 0; i != 10; ++i)
                calculator.probability(sequence);
        }

        Logger.setTraceEnabled(true);

        return usedPatterns;
    }
}
