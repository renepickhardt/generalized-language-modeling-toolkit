package de.glmtk.options;

import static java.util.Objects.requireNonNull;

public class DoubleOption extends Option {
    public static final String DEFAULT_ARGNAME = "FLOAT";

    public static double parseDouble(String doubleString,
                                     boolean mustBeProb,
                                     Option option) throws OptionException {
        double value;
        try {
            value = Double.valueOf(doubleString);
        } catch (NumberFormatException e) {
            throw new OptionException("Option %s could not be parsed as a "
                    + "floating point value: '%s'. Reason: %s.", option,
                    doubleString, e.getMessage());
        }

        if (mustBeProb && (value < 0.0 || value > 1.0))
            throw new OptionException("Option %s must be a valid probability "
                    + "in the range of [0.0, 1.0], got '%.2f' instead.",
                    option, value);

        return value;
    }

    private String argname;
    private boolean mustBeProb = false;
    private double value = 0.0;

    public DoubleOption(String shortopt,
                        String longopt,
                        String desc) {
        this(shortopt, longopt, desc, DEFAULT_ARGNAME);
    }

    public DoubleOption(String shortopt,
                        String longopt,
                        String desc,
                        String argname) {
        super(shortopt, longopt, desc);

        requireNonNull(argname);

        this.argname = argname;
    }

    /**
     * Must be in [0.0, 1.0].
     */
    public DoubleOption mustBeProbability() {
        mustBeProb = true;
        return this;
    }

    public DoubleOption defaultValue(double defaultValue) {
        value = defaultValue;
        return this;
    }

    @Override
    protected org.apache.commons.cli.Option createCommonsCliOption() {
        org.apache.commons.cli.Option commonsCliOption = new org.apache.commons.cli.Option(
                shortopt, longopt, true, desc);
        commonsCliOption.setArgName(argname);
        commonsCliOption.setArgs(1);
        return commonsCliOption;
    }

    @Override
    protected void handleParse(org.apache.commons.cli.Option commonsCliOption) throws OptionException {
        checkOnlyDefinedOnce();

        value = parseDouble(commonsCliOption.getValue(), mustBeProb, this);
    }

    public double getDouble() {
        return value;
    }
}
