package de.typology.smoothing;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.typology.utils.StringUtils;

public abstract class PropabilityCalculator {

    protected static Logger logger = LogManager
            .getLogger(PropabilityCalculator.class);

    protected Estimator estimator;

    public PropabilityCalculator(
            Estimator estimator) {
        this.estimator = estimator;
    }

    public abstract double propability(String sequence);

    protected List<String> getWords(String sequence) {
        return StringUtils.splitAtSpace(sequence);
    }

    protected void debugPropability(String sequence) {
        logger.debug(getClass().getSimpleName() + "#propability(" + sequence
                + ")");
    }

}
