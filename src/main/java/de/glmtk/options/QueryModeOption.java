package de.glmtk.options;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import de.glmtk.querying.probability.QueryMode;

public class QueryModeOption extends Option {
    public static final String DEFAULT_ARGNAME = "QUERY_MODE";

    //@formatter:off
    /* package */static final String EXPLANATION =
            "Where <%s> may be any of: \n" +
                    "  * sequence - Sequence\n" +
                    "  * <INT> - Fixed\n" +
                    "  * markov<INT> - Markov\n" +
                    "  * cond<INT> - Conditional\n";
    //@formatter:on

    /* package */static QueryMode parseQueryMode(String queryModeString,
                                                 Option option) throws OptionException {
        try {
            return QueryMode.forString(queryModeString);
        } catch (RuntimeException e) {
            throw new OptionException(
                    "Option %s got illegal query mode string '%s'.", option,
                    queryModeString);
        }
    }

    private String argname;
    private QueryMode value = null;

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
        value = defaultValue;
        return this;
    }

    @Override
    /* package */Multimap<String, String> registerExplanation() {
        return ImmutableMultimap.of(EXPLANATION, argname);
    }

    @Override
    /* package */org.apache.commons.cli.Option createCommonsCliOption() {
        org.apache.commons.cli.Option commonsCliOption = new org.apache.commons.cli.Option(
                shortopt, longopt, true, desc);
        commonsCliOption.setArgName(argname);
        commonsCliOption.setArgs(1);
        return commonsCliOption;
    }

    @Override
    protected void handleParse(org.apache.commons.cli.Option commonsCliOption) throws OptionException {
        checkOnlyDefinedOnce();

        value = parseQueryMode(commonsCliOption.getValue(), this);
    }

    public QueryMode getQueryMode() {
        return value;
    }
}
