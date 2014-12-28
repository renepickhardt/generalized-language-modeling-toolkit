package de.glmtk.querying.calculator;

import java.util.List;

import de.glmtk.common.NGram;

public class MarkovCalculator extends SentenceCalculator {

    private Integer order = 5;

    public void setOrder(int order) {
        if (order <= 0) {
            throw new IllegalArgumentException("Order must be > 0.");
        }
        this.order = order;
    }

    @Override
    protected List<SequenceAndHistory> computeQueries(List<String> words) {
        // TODO: what to do for conditional case?

        List<SequenceAndHistory> queries = super.computeQueries(words);

        for (int i = 0; i != queries.size(); ++i) {
            SequenceAndHistory query = queries.get(i);
            NGram h = query.history;
            if (h.size() >= order) {
                query.history = h.range(h.size() - order + 1, h.size());
            }
        }

        return queries;
    }

}
