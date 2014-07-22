package de.glmtk.smoothing.calculating;

import java.util.ArrayList;
import java.util.List;

import de.glmtk.patterns.PatternElem;
import de.glmtk.smoothing.NGram;
import de.glmtk.smoothing.estimating.Estimator;

/**
 * {@code P_Skip(a b c) = P(c | a b) * P(b _ | a) * P (a _ _ | )}
 */
public class SkipCalculator extends SequenceCalculator {

    @Override
    protected double
        calcPropability(Estimator estimator, List<String> sequence) {
        double result = 1;
        List<String> s, h = new ArrayList<String>(sequence);
        for (int i = 0; i != sequence.size(); ++i) {
            // build s
            s = new ArrayList<String>(i + 1);
            s.add(h.get(h.size() - 1));
            for (int j = 0; j != i; ++j) {
                s.add(PatternElem.SKIPPED_WORD);
            }

            // build h
            if (h.size() >= 1) {
                h = new ArrayList<String>(h.subList(0, h.size() - 1));
            } else {
                h = new ArrayList<String>();
            }

            result *= estimator.propability(new NGram(s), new NGram(h));
        }

        return result;
    }
}
