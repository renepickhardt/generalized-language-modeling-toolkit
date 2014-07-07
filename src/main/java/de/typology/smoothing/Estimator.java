package de.typology.smoothing;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.typology.utils.StringUtils;

public abstract class Estimator {

    protected static Logger logger = LoggerFactory.getLogger(Estimator.class);

    protected Corpus corpus;

    public Estimator(
            Corpus corpus) {
        this.corpus = corpus;
    }

    public final double propabilityCond(
            List<String> reqSequence,
            List<String> condSequence) {
        return propabilityCond(reqSequence, condSequence, 1);
    }

    protected abstract double propabilityCond(
            List<String> reqSequence,
            List<String> condSequence,
            int recDepth);

    protected void debugPropabilityCond(
            List<String> reqSequence,
            List<String> condSequence,
            int recDepth) {
        logger.debug(StringUtils.repeat("  ", recDepth)
                + getClass().getSimpleName() + "#propabilityCond("
                + reqSequence + ", " + condSequence + ")");
    }

}
