package de.typology.smoothing;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Estimator {

    protected static Logger logger = LoggerFactory.getLogger(Estimator.class);

    protected Corpus corpus;

    public Estimator(
            Corpus corpus) {
        this.corpus = corpus;
    }

    public abstract double propabilityCond(
            List<String> reqSequence,
            List<String> condSequence);

    protected void debugPropabilityCond(
            List<String> reqSequence,
            List<String> condSequence) {
        logger.debug("  " + getClass().getSimpleName() + "#propabilityCond("
                + reqSequence + ", " + condSequence + ")");
    }

}
