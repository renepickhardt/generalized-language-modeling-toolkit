package de.glmtk.options.custom;

import static com.google.common.collect.Lists.newArrayList;
import static de.glmtk.options.custom.EstimatorOption.EXPLANATION;
import static de.glmtk.options.custom.EstimatorOption.WEIGHTEDSUM_EXPLANATION;
import static de.glmtk.options.custom.EstimatorOption.parseEstimator;
import static de.glmtk.util.Strings.requireNotEmpty;
import static java.util.Objects.requireNonNull;

import java.util.List;

import com.google.common.collect.ImmutableList;

import de.glmtk.options.Option;
import de.glmtk.options.OptionException;
import de.glmtk.querying.estimator.Estimator;

public class EstimatorsOption extends Option {
    public static final String DEFAULT_ARGNAME = EstimatorOption.DEFAULT_ARGNAME;

    private Arg arg = new Arg(DEFAULT_ARGNAME, GREATER_ONE, EXPLANATION);
    private boolean requireWeightedSum = false;
    private List<Estimator> value = newArrayList();

    public EstimatorsOption(String shortopt,
                            String longopt,
                            String desc) {
        super(shortopt, longopt, desc);
        mayBeGivenRepeatedly = true;
    }

    public EstimatorsOption argName(String argName) {
        requireNonNull(argName);
        requireNotEmpty(argName);
        arg.name = argName;
        return this;
    }

    public EstimatorsOption needWeightedSum() {
        requireWeightedSum = true;
        if (arg.name.equals(DEFAULT_ARGNAME))
            arg.name = "WEIGHTEDSUM_ESTIMATOR";
        arg.explanation = WEIGHTEDSUM_EXPLANATION;
        return this;
    }

    public EstimatorsOption defaultValue(List<Estimator> defaultValue) {
        value = defaultValue;
        return this;
    }

    @Override
    protected List<Arg> arguments() {
        return ImmutableList.of(arg);
    }

    @Override
    protected void parse() throws OptionException {
        if (!given)
            value = newArrayList();

        for (String estimatorString : arg.values)
            value.add(parseEstimator(estimatorString, requireWeightedSum,
                    this));
    }

    public List<Estimator> getEstimators() {
        return value;
    }
}
