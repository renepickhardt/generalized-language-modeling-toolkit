package de.glmtk.options.custom;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static de.glmtk.options.custom.ArgmaxExecutorOption.EXPLANATION;
import static de.glmtk.options.custom.ArgmaxExecutorOption.parseArgmaxExecutor;

import java.util.List;

import com.google.common.collect.ImmutableList;

import de.glmtk.options.Option;
import de.glmtk.options.OptionException;

public class ArgmaxExecutorsOption extends Option {
    public static final String DEFAULT_ARGNAME = ArgmaxExecutorOption.DEFAULT_ARGNAME;

    private Arg arg = new Arg(DEFAULT_ARGNAME, GREATER_ONE, EXPLANATION);
    private List<String> value = newArrayList();

    public ArgmaxExecutorsOption(String shortopt,
                                 String longopt,
                                 String desc) {
        super(shortopt, longopt, desc);
        mayBeGivenRepeatedly = true;
    }

    public ArgmaxExecutorsOption argName(String argName) {
        checkNotNull(argName);
        checkArgument(!argName.isEmpty());
        arg.name = argName;
        return this;
    }

    public ArgmaxExecutorsOption defaultValue(List<String> defaultValue) {
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

        for (String executorString : arg.values)
            value.add(parseArgmaxExecutor(executorString, this));
    }

    public List<String> getArgmaxExecutors() {
        return value;
    }
}
