package de.typology.smoothing;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.typology.utils.StringUtils;

public abstract class PropabilityCalculator {

    protected static Logger logger = LoggerFactory
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
