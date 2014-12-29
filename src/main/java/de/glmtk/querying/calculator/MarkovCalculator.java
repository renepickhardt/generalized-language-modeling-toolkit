package de.glmtk.querying.calculator;

import java.util.List;

import de.glmtk.common.NGram;

public class MarkovCalculator extends SequenceCalculator {

    private int markovOrder;

    public MarkovCalculator(
            int markovOrder) {
        this.markovOrder = markovOrder;
    }

    public void setMarkovOrder(int markovOrder) {
        if (markovOrder <= 0) {
            throw new IllegalArgumentException(
                    "Markov markovOrder must be > 0.");
        }
        this.markovOrder = markovOrder;
    }

    @Override
    protected List<SequenceAndHistory> computeQueries(List<String> words) {
        // TODO: what to do for conditional case?

        List<SequenceAndHistory> queries = super.computeQueries(words);

        for (int i = 0; i != queries.size(); ++i) {
            SequenceAndHistory query = queries.get(i);
            NGram h = query.history;
            if (h.size() >= markovOrder) {
                query.history = h.range(h.size() - markovOrder + 1, h.size());
            }
        }

        return queries;
    }

}
