package de.glmtk.options;

import static java.util.Objects.requireNonNull;
import de.glmtk.querying.argmax.ArgmaxQueryExecutor;

public class ArgmaxExecutorOption extends Option {
    public static final String DEFAULT_ARGNAME = "ARGMAX_EXECUTOR";

    private String argname;
    private ArgmaxQueryExecutor defaultValue = null;

    public ArgmaxExecutorOption(String shortopt,
                                String longopt,
                                String desc) {
        this(shortopt, longopt, desc, DEFAULT_ARGNAME);
    }

    public ArgmaxExecutorOption(String shortopt,
                                String longopt,
                                String desc,
                                String argname) {
        super(shortopt, longopt, desc);

        requireNonNull(argname);

        this.argname = argname;
    }

    public ArgmaxExecutorOption defaultValue(ArgmaxQueryExecutor defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public ArgmaxQueryExecutor getArgmaxExecutor() {
        throw new UnsupportedOperationException();
    }
}
