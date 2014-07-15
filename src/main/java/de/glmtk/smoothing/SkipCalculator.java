package de.glmtk.smoothing;

import java.util.ArrayList;
import java.util.List;

import de.glmtk.patterns.PatternElem;

public class SkipCalculator extends PropabilityCalculator {

    public SkipCalculator(
            Estimator estimator) {
        super(estimator);
    }

    /**
     * {@code P(a b c) = Pcond(c | a b) * Pcond(b _ | a) * Pcond(a _ _ | )}
     */
    @Override
    public double propability(String sequence) {
        debugPropability(sequence);

        List<String> words = getWords(sequence);

        double result = 1;
        List<String> reqSequence, condSequence = new ArrayList<String>(words);
        for (int i = 0; i != words.size(); ++i) {
            // build reqSequence
            reqSequence = new ArrayList<String>(i + 1);
            reqSequence.add(condSequence.get(condSequence.size() - 1));
            for (int j = 0; j != i; ++j) {
                reqSequence.add(PatternElem.SKIPPED_WORD);
            }

            // build condSequence
            if (condSequence.size() >= 1) {
                condSequence =
                        new ArrayList<String>(condSequence.subList(0,
                                condSequence.size() - 1));
            } else {
                condSequence = new ArrayList<String>();
            }

            result *= estimator.propabilityCond(reqSequence, condSequence);
        }

        logger.debug("  result = " + result);
        return result;
    }
}
