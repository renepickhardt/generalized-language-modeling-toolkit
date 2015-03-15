package de.glmtk.querying.estimator.weightedsum;

import java.util.ArrayList;

import de.glmtk.common.NGram;
import de.glmtk.common.Pattern;
import de.glmtk.common.PatternElem;
import de.glmtk.querying.estimator.weightedsum.WeightedSumFunction.Summand;

public class WeightedSumFunction extends ArrayList<Summand> {
    private static final long serialVersionUID = -671750055618705155L;

    public class Summand {
        private NGram history;
        private double weight;

        public Summand(double weight,
                       NGram history) {
            this.history = history;
            this.weight = weight;
        }

        public double getWeight() {
            return weight;
        }

        public NGram getHistory() {
            return history;
        }

        @Override
        public String toString() {
            return "Summand [history=" + history + ", weight=" + weight + "]";
        }
    }

    public WeightedSumFunction() {
        super();
    }

    public WeightedSumFunction(int initialCapacity) {
        super(initialCapacity);
    }

    public void add(double weight,
                    NGram history) {
        add(new Summand(weight, history));
    }

    public Pattern[] getPatterns() {
        Pattern[] patterns = new Pattern[size()];
        for (int i = 0; i != size(); ++i)
            patterns[i] = get(i).history.getPattern().concat(PatternElem.CNT);
        return patterns;
    }

    public NGram[] getHistories() {
        NGram[] histories = new NGram[size()];
        for (int i = 0; i != size(); ++i)
            histories[i] = get(i).history;
        return histories;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(getClass().getSimpleName()).append(" [\n");
        for (Summand summand : this)
            result.append(String.format("  %-20s %e%n", summand.history,
                    summand.weight));
        result.append("]");
        return result.toString();
    }
}
