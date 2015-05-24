package de.glmtk.options;

import static java.util.Objects.requireNonNull;

public abstract class Option {
    protected static final String MULTIPLE_ARG_SUFFIX = "...";

    private String shortopt;
    private String longopt;
    private String desc;

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
}
