package de.glmtk.smoothing.legacy3;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.pattern.PatternElem;
import de.glmtk.smoothing.NGram;
import de.glmtk.smoothing.legacy3.estimating.Estimator;

/**
 * P(a b c)
 */
public class SequenceCalculator {

    private static final Logger LOGGER = LogManager
            .getLogger(SequenceCalculator.class);

    private CalculatingMode calculatingMode = null;

    public void setCalculatingMode(CalculatingMode calculatingMode) {
        this.calculatingMode = calculatingMode;
    }

    /**
     * if {@link #calculatingMode} = SKIP:<br>
     * {@code P(a b c) = P(c | a b) * P(b _ | a) * P (a _ _ | )}
     * 
     * <p>
     * if {@link #calculatingMode} = DELETE:<br>
     * {@code P(a b c) = P(c | a b) * P(b | a) * P(a |)}
     */
    public double propability(Estimator estimator, List<String> sequence) {
        LOGGER.debug("{}#propability({},{})", getClass().getSimpleName(),
                estimator.getClass().getSimpleName(), sequence);

        estimator.setCalculatingMode(calculatingMode);

        double result = 1;
        List<String> s, h = new ArrayList<String>(sequence);
        for (int i = 0; i != sequence.size(); ++i) {
            // build s
            s = new ArrayList<String>(i + 1);
            s.add(h.get(h.size() - 1));
            if (calculatingMode == CalculatingMode.SKIP) {
                for (int j = 0; j != i; ++j) {
                    s.add(PatternElem.SKIPPED_WORD);
                }
            }

            // build h
            if (h.size() >= 1) {
                h = new ArrayList<String>(h.subList(0, h.size() - 1));
            } else {
                h = new ArrayList<String>();
            }

            result *= estimator.probability(new NGram(s), new NGram(h));
        }

        LOGGER.debug("  result = {}", result);
        return result;
    }

}
