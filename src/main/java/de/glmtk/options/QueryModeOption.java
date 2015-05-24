package de.glmtk.options;

import static java.util.Objects.requireNonNull;
import de.glmtk.querying.probability.QueryMode;

public class QueryModeOption extends Option {
    public static final String DEFAULT_ARGNAME = "QUERY_MODE";

    private String argname;
    private QueryMode defaultValue = null;

    public QueryModeOption(String shortopt,
                           String longopt,
                           String desc) {
        this(shortopt, longopt, desc, DEFAULT_ARGNAME);
    }

    public QueryModeOption(String shortopt,
                           String longopt,
                           String desc,
                           String argname) {
        super(shortopt, longopt, desc);

        requireNonNull(argname);

        this.argname = argname;
    }

    public QueryModeOption defaultValue(QueryMode defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public QueryMode getQueryMode() {
        throw new UnsupportedOperationException();
    }
}
