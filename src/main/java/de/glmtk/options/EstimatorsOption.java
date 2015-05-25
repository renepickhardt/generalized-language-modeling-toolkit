package de.glmtk.options;

import static com.google.common.collect.Lists.newArrayList;
import static de.glmtk.options.EstimatorOption.EXPLANATION;
import static de.glmtk.options.EstimatorOption.WEIGHTEDSUM_EXPLANATION;
import static de.glmtk.options.EstimatorOption.parseEstimator;
import static java.util.Objects.requireNonNull;

import java.util.List;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import de.glmtk.querying.estimator.Estimator;

public class EstimatorsOption extends Option {
    public static final String DEFAULT_ARGNAME = EstimatorOption.DEFAULT_ARGNAME;

    private String argname;
    private boolean needWeightedSum = false;
    private List<Estimator> value = newArrayList();
    private boolean explicitDefault = false;

    public EstimatorsOption(String shortopt,
                            String longopt,
                            String desc) {
        this(shortopt, longopt, desc, DEFAULT_ARGNAME);
    }

    public EstimatorsOption(String shortopt,
                            String longopt,
                            String desc,
                            String argname) {
        super(shortopt, longopt, desc);

        requireNonNull(argname);

        this.argname = argname;
    }

    public EstimatorsOption needWeightedSum() {
        needWeightedSum = true;
        if (argname.equals(DEFAULT_ARGNAME))
            argname = "WEIGHTEDSUM_ESTIMATOR";
        return this;
    }

    public EstimatorsOption defaultValue(List<Estimator> defaultValue) {
        value = defaultValue;
        explicitDefault = true;
        return this;
    }

    @Override
    /* package */Multimap<String, String> registerExplanation() {
        if (needWeightedSum)
            return ImmutableMultimap.of(WEIGHTEDSUM_EXPLANATION, argname);
        return ImmutableMultimap.of(EXPLANATION, argname);
    }

    @Override
    /* package */org.apache.commons.cli.Option createCommonsCliOption() {
        org.apache.commons.cli.Option commonsCliOption = new org.apache.commons.cli.Option(
                shortopt, longopt, true, desc);
        commonsCliOption.setArgName(argname + MULTIPLE_ARG_SUFFIX);
        commonsCliOption.setArgs(org.apache.commons.cli.Option.UNLIMITED_VALUES);
        return commonsCliOption;
    }

    @Override
    protected void handleParse(org.apache.commons.cli.Option commonsCliOption) throws OptionException {
        if (explicitDefault) {
            explicitDefault = false;
            value = newArrayList();
        }

        for (String estimatorString : commonsCliOption.getValues())
            value.add(parseEstimator(estimatorString, needWeightedSum, this));
    }

    public List<Estimator> getEstimators() {
        return value;
    }
}
