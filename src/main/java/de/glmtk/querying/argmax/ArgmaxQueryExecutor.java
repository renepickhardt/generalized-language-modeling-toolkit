package de.glmtk.querying.argmax;

import java.util.Comparator;
import java.util.List;

public interface ArgmaxQueryExecutor {
    public static class ArgmaxResult {
        public static final Comparator<ArgmaxResult> COMPARATOR = new Comparator<ArgmaxResult>() {
            @Override
            public int compare(ArgmaxResult lhs,
                               ArgmaxResult rhs) {
                return Double.compare(lhs.probability, rhs.probability);
            }
        };

        private String sequence;
        private double probability;

        public ArgmaxResult(String sequence,
                            double probability) {
            this.sequence = sequence;
            this.probability = probability;
        }

        public String getSequence() {
            return sequence;
        }

        public double getProbability() {
            return probability;
        }

        @Override
        public String toString() {
            return String.format("%s\t%e", sequence, probability);
        }
    }

    public List<ArgmaxResult> queryArgmax(String history,
                                          int numResults);

    /**
     * @param prefix
     *            Enforce that any resulting argmax sequences begin with this
     *            prefix.
     */
    public List<ArgmaxResult> queryArgmax(String history,
                                          String prefix,
                                          int numResults);
}
