package de.glmtk.querying.calculator;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.common.NGram;
import de.glmtk.common.ProbMode;
import de.glmtk.querying.QueryType;
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

    private static final Logger LOGGER = LogManager.getFormatterLogger(Calculator.class);

    public static final Calculator forQueryTypeString(String queryTypeString) {
        switch (QueryType.fromString(queryTypeString)) {
            case COND:
                return new CondCalculator();

            case SEQUENCE:
                return new SequenceCalculator();

            case MARKOV:
                return new MarkovCalculator(
                        QueryType.getMarkovOrder(queryTypeString));

            default:
                throw new IllegalStateException();
        }
    }

    protected Estimator estimator = null;
    protected ProbMode probMode = null;

    public void setEstimator(Estimator estimator) {
        this.estimator = estimator;
        if (probMode != null)
            estimator.setProbMode(probMode);
    }

    public void setProbMode(ProbMode probMode) {
        this.probMode = probMode;
        if (estimator != null)
            estimator.setProbMode(probMode);
    }

    public double probability(List<String> words) {
        LOGGER.debug("%s#probability(%s,%s)", getClass().getSimpleName(),
                estimator.getClass().getSimpleName(), words);

        estimator.setProbMode(probMode);

        List<SequenceAndHistory> queries = computeQueries(words);

        double result = 1.0;
        for (SequenceAndHistory query : queries)
            result *= estimator.probability(query.sequence, query.history);

        LOGGER.debug("  result = %f", result);
        return result;
    }

    protected abstract List<SequenceAndHistory> computeQueries(List<String> words);
}
