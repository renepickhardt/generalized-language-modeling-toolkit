package de.glmtk.querying.calculator;

import java.util.ArrayList;
import java.util.List;

import de.glmtk.utils.NGram;

public class CondCalculator extends Calculator {

    @Override
    protected List<SequenceAndHistory> computeQueries(List<String> words) {
        List<SequenceAndHistory> queries = new ArrayList<SequenceAndHistory>();
        String s = words.get(words.size() - 1);
        List<String> h = words.subList(0, words.size() - 1);
        queries.add(new SequenceAndHistory(new NGram(s), new NGram(h)));
        return queries;
    }

}
