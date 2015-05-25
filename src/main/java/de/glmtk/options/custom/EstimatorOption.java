package de.glmtk.options.custom;

import static de.glmtk.util.Maps.maxKeyLength;
import static de.glmtk.util.StringUtils.join;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.ImmutableList;

import de.glmtk.options.Option;
import de.glmtk.options.OptionException;
import de.glmtk.querying.estimator.Estimator;
import de.glmtk.querying.estimator.Estimators;
import de.glmtk.querying.estimator.weightedsum.WeightedSumEstimator;

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

    protected static final String EXPLANATION;
    protected static final String WEIGHTEDSUM_EXPLANATION;
    static {
        int longestAbbr = maxKeyLength(VALUES);

        StringBuilder exp = new StringBuilder();
        StringBuilder wsumExp = new StringBuilder();
        exp.append("Where <%s> may be any of:\n");
        wsumExp.append("Where <%s> may be any of:\n");
        for (Entry<String, Estimator> estimator : VALUES.entrySet()) {
            String line = format("  * %-" + longestAbbr + "s - %s\n",
                    estimator.getKey(), estimator.getValue().getName());

            exp.append(line);
            if (estimator.getValue() instanceof WeightedSumEstimator)
                wsumExp.append(line);
        }

        EXPLANATION = exp.toString();
        WEIGHTEDSUM_EXPLANATION = wsumExp.toString();
    }

    public static final Estimator parseEstimator(String estimatorString,
                                                 boolean constrainWeightedSum,
                                                 Option option) throws OptionException {
        requireNonNull(estimatorString);
        requireNonNull(option);

        Estimator estimator = VALUES.get(estimatorString.toUpperCase());
        if (estimator == null)
            throw new OptionException(
                    "Option %s estimator not recognized: '%s'. Valid Values: %s.",
                    option, estimatorString, join(VALUES.keySet(), ", "));
        if (constrainWeightedSum
                && !(estimator instanceof WeightedSumEstimator))
            throw new OptionException("Option %s estimator needs to be a "
                    + "weighted sum estimator.", option);
        return estimator;
    }

    private Arg arg = new Arg(DEFAULT_ARGNAME, 1, EXPLANATION);
    private boolean constrainWeightedSum = false;
    private Estimator value = null;

    public EstimatorOption(String shortopt,
                           String longopt,
                           String desc) {
        super(shortopt, longopt, desc);
    }

    public EstimatorOption argName(String argName) {
        requireNonNull(argName);
        arg.name = argName;
        return this;
    }

    public EstimatorOption constrainWeightedSum() {
        constrainWeightedSum = true;
        if (arg.name.equals(DEFAULT_ARGNAME))
            arg.name = "WEIGHTEDSUM_ESTIMATOR";
        arg.explanation = WEIGHTEDSUM_EXPLANATION;
        return this;
    }

    public EstimatorOption defaultValue(Estimator defaultValue) {
        value = defaultValue;
        return this;
    }

    @Override
    protected List<Arg> arguments() {
        return ImmutableList.of(arg);
    }

    @Override
    protected void parse() throws OptionException {
        value = parseEstimator(arg.values.get(0), constrainWeightedSum, this);
    }

    public Estimator getEstimator() {
        return value;
    }
}
