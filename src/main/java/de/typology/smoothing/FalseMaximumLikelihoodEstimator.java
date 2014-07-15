package de.typology.smoothing;

import java.util.ArrayList;
import java.util.List;

public class FalseMaximumLikelihoodEstimator extends MaximumLikelihoodEstimator {

    /**
     * {@code sequence = condSequence + reqSequence}
     */
    @Override
    protected List<String> getSequence(
            List<String> reqSequence,
            List<String> condSequence) {
        int n = reqSequence.size() + condSequence.size() - 1;

        List<String> sequence = new ArrayList<String>(n);
        sequence.addAll(condSequence);
        sequence.addAll(reqSequence);

        return sequence;
    }

    /**
     * {@code history = condSequence}
     */
    @Override
    protected List<String> getHistory(
            List<String> reqSequence,
            List<String> condSequence) {
        return condSequence;
    }

}
