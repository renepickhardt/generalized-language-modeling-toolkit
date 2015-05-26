package de.glmtk.options;

import static de.glmtk.util.Strings.requireNotEmpty;
import static java.util.Objects.requireNonNull;

import java.util.List;

import com.google.common.collect.ImmutableList;

public class DoubleOption extends Option {
    public static final String DEFAULT_ARGNAME = "FLOAT";

    public static double parseDouble(String doubleString,
                                     boolean requireProbability,
                                     Option option) throws OptionException {
        requireNonNull(doubleString);
        requireNonNull(option);

        double value;
        try {
            value = Double.parseDouble(doubleString);
        } catch (NumberFormatException e) {
            throw new OptionException("%s could not be parsed as a "
                    + "floating point value: '%s'. Reason: %s.", option,
                    doubleString, e.getMessage());
        }

        if (requireProbability && (value < 0.0 || value > 1.0))
            throw new OptionException("%s must be a valid probability "
                    + "in the range of [0.0, 1.0], got '%.2f' instead.",
                    option, value);

        return value;
    }

    private Arg arg = new Arg(DEFAULT_ARGNAME, 1);
    private boolean requireProbability = false;
    private double value = 0.0;

    public DoubleOption(String shortopt,
                        String longopt,
                        String desc) {
        super(shortopt, longopt, desc);
    }

    public DoubleOption argName(String argName) {
        requireNonNull(argName);
        requireNotEmpty(argName);
        arg.name = argName;
        return this;
    }

    /**
     * Must be in [0.0, 1.0].
     */
    public DoubleOption requireProbability() {
        requireProbability = true;
        return this;
    }

    public DoubleOption defaultValue(double defaultValue) {
        value = defaultValue;
        return this;
    }

    @Override
    protected List<Arg> arguments() {
        return ImmutableList.of(arg);
    }

    @Override
    protected void parse() throws OptionException {
        value = parseDouble(arg.value, requireProbability, this);
    }

    public double getDouble() {
        return value;
    }
}
