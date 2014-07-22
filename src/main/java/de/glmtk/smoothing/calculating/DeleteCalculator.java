package de.glmtk.smoothing.calculating;

import java.util.ArrayList;
import java.util.List;

import de.glmtk.smoothing.NGram;
import de.glmtk.smoothing.estimating.Estimator;

/**
 * {@code P_Delete(a b c) = P(c | a b) * P(b | a) * P(a |)}
 */
public class DeleteCalculator extends SequenceCalculator {

    @Override
    protected double
        calcPropability(Estimator estimator, List<String> sequence) {
        double result = 1;
        List<String> s, h = new ArrayList<String>(sequence);
        for (int i = 0; i != sequence.size(); ++i) {
            // build s
            s = new ArrayList<String>(i + 1);
            s.add(h.get(h.size() - 1));

            // build h
            if (h.size() >= 1) {
                h = new ArrayList<String>(h.subList(0, h.size() - 1));
            } else {
                h = new ArrayList<String>();
            }

            result *= estimator.probability(new NGram(s), new NGram(h));
        }

        return result;
    }

}
