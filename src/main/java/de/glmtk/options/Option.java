package de.glmtk.options;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import com.google.common.collect.Multimap;

public abstract class Option {
    protected static final String MULTIPLE_ARG_SUFFIX = "...";

    protected String shortopt;
    protected String longopt;
    protected String desc;
    private boolean parsed = false;

    /**
     * @param shortopt
     *            May be {@code null}.
     */
    public Option(String shortopt,
                  String longopt,
                  String desc) {
        requireNonNull(longopt);
        requireNonNull(desc);

        this.shortopt = shortopt;
        this.longopt = longopt;
        this.desc = desc;
    }

    @Override
    public String toString() {
        if (shortopt == null)
            return format("--%s", longopt);
        return format("-%s (--%s)", shortopt, longopt);
    }

    public boolean wasGiven() {
        return parsed;
    }

    /* package */Multimap<String, String> registerExplanation() {
        return null;
    }

    /* package */abstract org.apache.commons.cli.Option createCommonsCliOption();

    /* package */void parse(org.apache.commons.cli.Option commonsCliOption) throws OptionException {
        requireNonNull(commonsCliOption);
        handleParse(commonsCliOption);
        parsed = true;
    }

    protected abstract void handleParse(org.apache.commons.cli.Option commonsCliOption) throws OptionException;

    protected void checkOnlyDefinedOnce() throws OptionException {
        if (parsed)
            throw new OptionException(
                    "Option %s must not be specified more than once.", this);
    }
}
