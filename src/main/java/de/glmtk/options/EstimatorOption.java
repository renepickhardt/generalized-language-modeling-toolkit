package de.glmtk.options;

import static de.glmtk.util.Maps.maxKeyLength;
import static de.glmtk.util.StringUtils.join;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import de.glmtk.querying.estimator.Estimator;
import de.glmtk.querying.estimator.Estimators;

public class EstimatorOption extends Option {
    public static final String DEFAULT_ARGNAME = "ESTIMATOR";

    private static final Map<String, Estimator> VALUES = new LinkedHashMap<>();
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

    /* package */static final String EXPLANATION;
    static {
        int longestAbbr = maxKeyLength(VALUES);

        StringBuilder sb = new StringBuilder();
        sb.append("Where <%s> may be any of:\n");
        for (Entry<String, Estimator> estimator : VALUES.entrySet())
            sb.append(format("  * %-" + longestAbbr + "s - %s\n",
                    estimator.getKey(), estimator.getValue().getName()));

        EXPLANATION = sb.toString();
    }

    /* package */static final Estimator parseEstimator(String estimatorString,
                                                       Option option) throws OptionException {
        Estimator estimator = VALUES.get(estimatorString.toUpperCase());
        if (estimator == null)
            throw new OptionException(
                    "Option %s estimator not recognized: '%s'. Valid Values: %s.",
                    option, estimatorString, join(VALUES.keySet(), ", "));
        return estimator;
    }

    private String argname;
    private Estimator value = null;

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
        value = defaultValue;
        return this;
    }

    @Override
    /* package */Multimap<String, String> registerExplanation() {
        return ImmutableMultimap.of(EXPLANATION, argname);
    }

    @Override
    /* package */org.apache.commons.cli.Option createCommonsCliOption() {
        org.apache.commons.cli.Option commonsCliOption = new org.apache.commons.cli.Option(
                shortopt, longopt, true, desc);
        commonsCliOption.setArgName(argname);
        commonsCliOption.setArgs(1);
        return commonsCliOption;
    }

    @Override
    protected void handleParse(org.apache.commons.cli.Option commonsCliOption) throws OptionException {
        checkOnlyDefinedOnce();
        value = parseEstimator(commonsCliOption.getValue(), this);
    }

    public Estimator getEstimator() {
        return value;
    }
}
