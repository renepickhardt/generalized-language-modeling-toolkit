package de.glmtk.options.custom;

import static com.google.common.collect.Lists.newArrayList;
import static de.glmtk.options.custom.ArgmaxExecutorOption.EXPLANATION;
import static de.glmtk.options.custom.ArgmaxExecutorOption.parseArgmaxExecutor;
import static java.util.Objects.requireNonNull;

import java.util.List;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import de.glmtk.options.Option;
import de.glmtk.options.OptionException;

public class ArgmaxExecutorsOption extends Option {
    public static final String DEFAULT_ARGNAME = ArgmaxExecutorOption.DEFAULT_ARGNAME;

    private String argname;
    private List<String> value = newArrayList();
    private boolean explicitDefault = false;

    public ArgmaxExecutorsOption(String shortopt,
                                 String longopt,
                                 String desc) {
        this(shortopt, longopt, desc, DEFAULT_ARGNAME);
    }

    public ArgmaxExecutorsOption(String shortopt,
                                 String longopt,
                                 String desc,
                                 String argname) {
        super(shortopt, longopt, desc);

        requireNonNull(argname);

        this.argname = argname;
    }

    public ArgmaxExecutorsOption defaultValue(List<String> defaultValue) {
        value = defaultValue;
        explicitDefault = true;
        return this;
    }

    @Override
    protected Multimap<String, String> registerExplanation() {
        return ImmutableMultimap.of(EXPLANATION, argname);
    }

    @Override
    protected org.apache.commons.cli.Option createCommonsCliOption() {
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

        for (String executorString : commonsCliOption.getValues())
            value.add(parseArgmaxExecutor(executorString, this));
    }

    public List<String> getArgmaxExecutors() {
        return value;
    }
}
