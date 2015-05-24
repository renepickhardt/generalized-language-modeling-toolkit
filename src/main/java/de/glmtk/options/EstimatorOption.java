package de.glmtk.options;

import static java.util.Objects.requireNonNull;
import de.glmtk.querying.estimator.Estimator;

public class EstimatorOption extends Option {
    public static final String DEFAULT_ARGNAME = "ESTIMATOR";

    private String argname;
    private Estimator defaultValue = null;

    public EstimatorOption(String shortopt,
                           String longopt,
                           String desc) {
        this(shortopt, longopt, desc, DEFAULT_ARGNAME);
    }

    public EstimatorOption(String shortopt,
                           String longopt,
                           String desc,
                           String argname) {
        super(shortopt, longopt, desc);

        requireNonNull(argname);

        this.argname = argname;
    }

    public EstimatorOption defaultValue(Estimator defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public Estimator getEstimator() {
        throw new UnsupportedOperationException();
    }
}
