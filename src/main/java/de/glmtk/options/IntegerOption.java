package de.glmtk.options;

import static java.util.Objects.requireNonNull;

public class IntegerOption extends Option {
    public static final String DEFAULT_ARGNAME = "INT";

    private String argname;
    private boolean mustBePositive = false;
    private boolean mustNotBeZero = false;
    private int value = 0;

    public IntegerOption(String shortopt,
                         String longopt,
                         String desc) {
        this(shortopt, longopt, desc, DEFAULT_ARGNAME);
    }

    public IntegerOption(String shortopt,
                         String longopt,
                         String desc,
                         String argname) {
        super(shortopt, longopt, desc);

        requireNonNull(argname);

        this.argname = argname;
    }

    /**
     * Allows zero and greater integers.
     */
    public IntegerOption mustBePositive() {
        mustBePositive = true;
        improveArgname();
        return this;
    }

    public IntegerOption mustNotBeZero() {
        mustNotBeZero = true;
        improveArgname();
        return this;
    }

    private void improveArgname() {
        if (argname.equals(DEFAULT_ARGNAME))
            if (mustBePositive && mustNotBeZero)
                argname += ">0";
            else if (mustBePositive)
                argname += ">=0";
            else if (mustNotBeZero)
                argname += "!=0";
    }

    public IntegerOption defaultValue(int defaultValue) {
        value = defaultValue;
        return this;
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
    /* package */void parse(org.apache.commons.cli.Option commonsCliOption) throws OptionException {
        checkOnlyDefinedOnce();

        try {
            value = Integer.valueOf(commonsCliOption.getValue());
        } catch (NumberFormatException e) {
            throw new OptionException("Option %s could not be parsed as an "
                    + "integer: '%s'. Reason: %s.", this,
                    commonsCliOption.getValue(), e.getMessage());
        }

        if (mustBePositive && value < 0)
            throw new OptionException(
                    "Option %s must not be negative, got '%d' instead.", this,
                    value);

        if (mustNotBeZero && value == 0)
            throw new OptionException("Option %s must not be zero.", this);
    }

    public int getInt() {
        return value;
    }
}
