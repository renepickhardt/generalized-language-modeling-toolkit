package de.glmtk.querying.estimator.weightedsum;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import de.glmtk.common.NGram;
import de.glmtk.common.Pattern;
import de.glmtk.common.PatternElem;
import de.glmtk.querying.estimator.weightedsum.WeightedSumFunction.Summand;

public class WeightedSumFunction extends ArrayList<Summand> {
    private static final long serialVersionUID = -671750055618705155L;

    public class Summand {
        private NGram history;
        private boolean absolute;
        private double weight;
        private boolean discounted;

        public Summand(double weight,
                       NGram history,
                       boolean absolute,
                       boolean discounted) {
            this.history = history;
            this.absolute = absolute;
            this.weight = weight;
            this.discounted = discounted;
        }

        public double getWeight() {
            return weight;
        }

        public NGram getHistory() {
            return history;
        }

        public boolean isAbsolute() {
            return absolute;
        }

        public boolean isDiscounted() {
            return discounted;
        }

        @Override
        public String toString() {
            return "Summand [history=" + history + ", absolute=" + absolute
                    + ", weight=" + weight + ", discounted=" + discounted + "]";
        }
    }

    public WeightedSumFunction() {
        super();
    }

    public WeightedSumFunction(int initialCapacity) {
        super(initialCapacity);
    }

    public void add(double weight,
                    NGram history,
                    boolean absolute,
                    boolean discounted) {
        add(new Summand(weight, history, absolute, discounted));
    }

    public Collection<Pattern> getNeededPatterns() {
        Set<Pattern> patterns = new HashSet<>();
        for (Summand summand : this)
            patterns.add(summand.history.getPattern().concat(PatternElem.CNT));
        return patterns;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(getClass().getSimpleName()).append(" [\n");
        for (Summand summand : this)
            result.append(String.format("  %-20s %e %b %b %n", summand.history,
                    summand.weight, summand.absolute, summand.discounted));
        result.append("]");
        return result.toString();
    }
}
