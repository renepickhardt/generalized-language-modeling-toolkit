package de.glmtk.smoothing.legacy2;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.utils.StringUtils;

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
        return StringUtils.splitAtChar(sequence, ' ');
    }

    protected void debugPropability(String sequence) {
        logger.debug(getClass().getSimpleName() + "#propability(" + sequence
                + ")");
    }

}
