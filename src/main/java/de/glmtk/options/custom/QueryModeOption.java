package de.glmtk.options.custom;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import com.google.common.collect.ImmutableList;

import de.glmtk.options.Option;
import de.glmtk.options.OptionException;
import de.glmtk.querying.probability.QueryMode;


public class QueryModeOption extends Option {
    public static final String DEFAULT_ARGNAME = "QUERY_MODE";

    //@formatter:off
    protected static final String EXPLANATION =
            "Where <%s> may be any of: \n" +
                    "  * sequence - Sequence\n" +
                    "  * <INT> - Fixed\n" +
                    "  * markov<INT> - Markov\n" +
                    "  * cond<INT> - Conditional\n";
    //@formatter:on

    public static QueryMode parseQueryMode(String queryModeString,
                                           Option option)
                                                   throws OptionException {
        checkNotNull(queryModeString);
        checkNotNull(option);

        try {
            return QueryMode.forString(queryModeString);
        } catch (RuntimeException e) {
            throw new OptionException("%s got illegal query mode string '%s'.",
                option, queryModeString);
        }
    }

    private Arg arg = new Arg(DEFAULT_ARGNAME, 1, EXPLANATION);
    private QueryMode value = null;

    public QueryModeOption(String shortopt,
                           String longopt,
                           String desc) {
        super(shortopt, longopt, desc);
    }

    public QueryModeOption argName(String argName) {
        checkNotNull(argName);
        checkArgument(!argName.isEmpty());
        arg.name = argName;
        return this;
    }

    public QueryModeOption defaultValue(QueryMode defaultValue) {
        value = defaultValue;
        return this;
    }

    @Override
    protected List<Arg> arguments() {
        return ImmutableList.of(arg);
    }

    @Override
    protected void parse() throws OptionException {
        value = parseQueryMode(arg.value, this);
    }

    public QueryMode getQueryMode() {
        return value;
    }
}
