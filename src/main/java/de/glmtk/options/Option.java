package de.glmtk.options;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public abstract class Option {
    protected static final String MULTIPLE_ARG_SUFFIX = "...";

    protected String shortopt;
    protected String longopt;
    protected String desc;
    private boolean firstDefinition = true;

    /**
     * @param shortopt
     *            May be {@code null}.
     */
    public Option(String shortopt,
                  String longopt,
                  String desc) {
        requireNonNull(longopt);

        this.shortopt = shortopt;
        this.longopt = longopt;
        this.desc = desc;
    }

    public boolean wasGiven() {
        throw new UnsupportedOperationException();
    }

    /* package */abstract org.apache.commons.cli.Option createCommonsCliOption();

    /* package */abstract void parse(org.apache.commons.cli.Option commonsCliOption) throws OptionException;

    protected void checkOnlyDefinedOnce() throws OptionException {
        if (firstDefinition) {
            firstDefinition = false;
            return;
        }

        throw new OptionException(
                "Option %s must not be specified more than once.", this);
    }

    @Override
    public String toString() {
        if (shortopt == null)
            return format("--%s", longopt);
        return format("-%s (--%s)", shortopt, longopt);
    }
}
