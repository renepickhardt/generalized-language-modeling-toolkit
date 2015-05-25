package de.glmtk.options;

import static de.glmtk.options.EstimatorOption.parseEstimator;
import static de.glmtk.util.revamp.ListUtils.list;
import static java.util.Objects.requireNonNull;

import java.util.List;

import de.glmtk.querying.estimator.Estimator;

public class EstimatorsOption extends Option {
    public static final String DEFAULT_ARGNAME = EstimatorOption.DEFAULT_ARGNAME;

    private String argname;
    private List<Estimator> value = list();
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

    public EstimatorsOption defaultValue(List<Estimator> defaultValue) {
        value = defaultValue;
        explicitDefault = true;
        return this;
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
    /* package */void parse(org.apache.commons.cli.Option commonsCliOption) throws OptionException {
        if (explicitDefault) {
            explicitDefault = false;
            value = list();
        }

        for (String estimatorString : commonsCliOption.getValues())
            value.add(parseEstimator(estimatorString, this));
    }

    public List<Estimator> getEstimators() {
        return value;
    }
}
