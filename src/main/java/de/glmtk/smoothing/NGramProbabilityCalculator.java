package de.glmtk.smoothing;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.pattern.PatternElem;
import de.glmtk.smoothing.estimator.Estimator;

public class NGramProbabilityCalculator {

    private static final Logger LOGGER = LogManager
            .getLogger(NGramProbabilityCalculator.class);

    private Estimator estimator = null;

    private ProbMode probMode = null;

    public void setEstimator(Estimator estimator) {
        this.estimator = estimator;
        if (probMode != null) {
            estimator.setProbMode(probMode);
        }
    }

    public void setProbMode(ProbMode probMode) {
        this.probMode = probMode;
        if (estimator != null) {
            estimator.setProbMode(probMode);
        }
    }

    /**
     * If {@link #probMode} = {@link ProbMode#COND}:<br>
     * {@code P(a b c) = P(c | a b) * P(b _ | a) * P (a _ _ | )}
     * 
     * <p>
     * If {@link #probMode} = {@link ProbMode#MARG}:<br>
     * {@code P(a b c) = P(c | a b) * P(b | a) * P(a |)}
     */
    public double probability(List<String> sequence) {
        LOGGER.debug("{}#probability({},{})", getClass().getSimpleName(),
                estimator.getClass().getSimpleName(), sequence);

        estimator.setProbMode(probMode);

        double result = 1;
        List<String> s, h = new ArrayList<String>(sequence);
        for (int i = 0; i != sequence.size(); ++i) {
            // build s
            s = new ArrayList<String>(i + 1);
            s.add(h.get(h.size() - 1));
            if (probMode == ProbMode.COND) {
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
