package de.glmtk.querying.calculator;

import static de.glmtk.utils.PatternElem.SKP_WORD;

import java.util.ArrayList;
import java.util.List;

import de.glmtk.querying.ProbMode;
import de.glmtk.utils.NGram;

public class SentenceCalculator extends Calculator {

    /**
     * If {@link #probMode} = {@link ProbMode#COND}:<br>
     * {@code P(a b c) = P(c | a b) * P(b _ | a) * P (a _ _ | )}
     *
     * <p>
     * If {@link #probMode} = {@link ProbMode#MARG}:<br>
     * {@code P(a b c) = P(c | a b) * P(b | a) * P(a |)}
     */
    @Override
    protected List<SequenceAndHistory> computeQueries(List<String> words) {
        List<SequenceAndHistory> queries = new ArrayList<SequenceAndHistory>();

        List<String> s, h = new ArrayList<String>(words);
        for (int i = 0; i != words.size(); ++i) {
            // build s
            s = new ArrayList<String>(i + 1);
            s.add(h.get(h.size() - 1));
            if (probMode == ProbMode.COND) {
                for (int j = 0; j != i; ++j) {
                    s.add(SKP_WORD);
                }
            }

            // build h
            if (h.size() >= 1) {
                h = new ArrayList<String>(h.subList(0, h.size() - 1));
            } else {
                h = new ArrayList<String>();
            }

            queries.add(new SequenceAndHistory(new NGram(s), new NGram(h)));
        }

        return queries;
    }
}
