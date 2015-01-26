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

package de.glmtk.querying.calculator;

import java.util.List;

import de.glmtk.common.NGram;
import de.glmtk.common.ProbMode;
import de.glmtk.exceptions.SwitchCaseNotImplementedException;
import de.glmtk.logging.Logger;
import de.glmtk.querying.QueryMode;
import de.glmtk.querying.estimator.Estimator;

public abstract class Calculator {
    protected static class SequenceAndHistory {
        public NGram sequence;
        public NGram history;

        public SequenceAndHistory(NGram sequence,
                                  NGram history) {
            this.sequence = sequence;
            this.history = history;
        }

        @Override
        public String toString() {
            return "(" + sequence + " | " + history + ")";
        }
    }

    private static final Logger LOGGER = Logger.get(Calculator.class);

    public static final Calculator forQueryMode(QueryMode queryMode) {
        switch (queryMode.getType()) {
            case SEQUENCE:
                return new SequenceCalculator();

            case FIXED:
                return new SequenceCalculator();

            case MARKOV:
                return new MarkovCalculator(queryMode.getOrder());

            case COND:
                return new CondCalculator();

            default:
                throw new SwitchCaseNotImplementedException();
        }
    }

    protected Estimator estimator = null;
    protected ProbMode probMode = null;

    public void setEstimator(Estimator estimator) {
        this.estimator = estimator;
        if (probMode == null)
            probMode = estimator.getProbMode();
        else
            estimator.setProbMode(probMode);
    }

    public void setProbMode(ProbMode probMode) {
        this.probMode = probMode;
        if (estimator != null)
            estimator.setProbMode(probMode);
    }

    public double probability(List<String> words) {
        LOGGER.trace("%s#probability(%s,%s)", getClass().getSimpleName(),
                estimator.getClass().getSimpleName(), words);

        estimator.setProbMode(probMode);

        List<SequenceAndHistory> queries = computeQueries(words);

        double result = 1.0;
        for (SequenceAndHistory query : queries)
            result *= estimator.probability(query.sequence, query.history);

        LOGGER.trace("  result = %e", result);
        return result;
    }

    protected abstract List<SequenceAndHistory> computeQueries(List<String> words);
}
