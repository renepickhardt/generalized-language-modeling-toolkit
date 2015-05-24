package de.glmtk.options;

import static de.glmtk.util.revamp.ListUtils.list;
import static java.util.Objects.requireNonNull;

import java.util.List;

import de.glmtk.querying.argmax.ArgmaxQueryExecutor;

public class ArgmaxExecutorsOption extends Option {
    public static final String DEFAULT_ARGNAME = ArgmaxExecutorOption.DEFAULT_ARGNAME;

    private String argname;
    private List<ArgmaxExecutorOption> defaultValue = list();

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

    public ArgmaxExecutorsOption defaultValue(List<ArgmaxQueryExecutor> defaultValue) {
        this.defaultValue = this.defaultValue;
        return this;
    }

    public List<ArgmaxQueryExecutor> getArgmaxExecutors() {
        throw new UnsupportedOperationException();
    }
}
