package de.glmtk.smoothing.calculating;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.smoothing.estimating.Estimator;

/**
 * P(a b c)
 */
public abstract class SequenceCalculator {

    private final static Logger LOGGER = LogManager
            .getLogger(SequenceCalculator.class);

    public double propability(Estimator estimator, List<String> sequence) {
        LOGGER.debug("{}#propability({},{})", getClass().getSimpleName(),
                estimator.getClass().getSimpleName(), sequence);
        double result = calcPropability(estimator, sequence);
        LOGGER.debug("  result = {}", result);
        return result;
    }

    protected abstract double calcPropability(
            Estimator estimator,
            List<String> sequence);

}
