package de.glmtk.options;

import static java.util.Objects.requireNonNull;

import java.util.LinkedHashMap;
import java.util.Map;

import de.glmtk.querying.estimator.Estimator;
import de.glmtk.querying.estimator.Estimators;

public class EstimatorOption extends Option {
    public static final String DEFAULT_ARGNAME = "ESTIMATOR";
    /* package */static final Map<String, Estimator> VALUES = new LinkedHashMap<>();
    static {
        VALUES.put("MLE", Estimators.FAST_MLE);
        VALUES.put("MKN", Estimators.WEIGHTEDSUM_MKN);
        VALUES.put("FMKN", Estimators.FAST_MKN);
        VALUES.put("MKNS", Estimators.FAST_MKN_SKP);
        VALUES.put("MKNA", Estimators.FAST_MKN_ABS);
        VALUES.put("GLM", Estimators.WEIGHTEDSUM_GLM);
        VALUES.put("FGLM", Estimators.FAST_GLM);
        VALUES.put("GLMD", Estimators.FAST_GLM_DEL);
        VALUES.put("GLMDF", Estimators.FAST_GLM_DEL_FRONT);
        VALUES.put("GLMSD", Estimators.FAST_GLM_SKP_AND_DEL);
        VALUES.put("GLMA", Estimators.FAST_GLM_ABS);
        VALUES.put("WSA", Estimators.WEIGHTEDSUM_AVERAGE);
    }

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
