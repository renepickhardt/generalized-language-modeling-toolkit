package de.glmtk.options;

import static java.util.Objects.requireNonNull;

import java.util.List;

import com.google.common.collect.ImmutableList;

public class IntegerOption extends Option {
    public static final String DEFAULT_ARGNAME = "INT";

    public static final int parseInteger(String integerString,
                                         boolean constrainPositive,
                                         boolean constrainNotZero,
                                         Option option) throws OptionException {
        requireNonNull(integerString);
        requireNonNull(option);

        int value;
        try {
            value = Integer.valueOf(integerString);
        } catch (NumberFormatException e) {
            throw new OptionException("Option %s could not be parsed as an "
                    + "integer: '%s'. Reason: %s.", option, integerString,
                    e.getMessage());
        }

        if (constrainPositive && value < 0)
            throw new OptionException(
                    "Option %s must not be negative, got '%d' instead.",
                    option, value);

        if (constrainNotZero && value == 0)
            throw new OptionException("Option %s must not be zero.", option);

        return value;
    }

    private Arg arg = new Arg(DEFAULT_ARGNAME, 1);
    private boolean constrainPositive = false;
    private boolean constrainNotZero = false;
    private int value = 0;

    public IntegerOption(String shortopt,
                         String longopt,
                         String desc) {
        super(shortopt, longopt, desc);
    }

    public IntegerOption argName(String argName) {
        requireNonNull(argName);
        arg.name = argName;
        return this;
    }

    /**
     * Allows zero and greater integers.
     */
    public IntegerOption constainPositive() {
        constrainPositive = true;
        improveArgName();
        return this;
    }

    public IntegerOption contrainNotZero() {
        constrainNotZero = true;
        improveArgName();
        return this;
    }

    private void improveArgName() {
        if (arg.name.equals(DEFAULT_ARGNAME))
            if (constrainPositive && constrainNotZero)
                arg.name += ">0";
            else if (constrainPositive)
                arg.name += ">=0";
            else if (constrainNotZero)
                arg.name += "!=0";
    }

    public IntegerOption defaultValue(int defaultValue) {
        value = defaultValue;
        return this;
    }

    @Override
    protected List<Arg> arguments() {
        return ImmutableList.of(arg);
    }

    @Override
    protected void parse() throws OptionException {
        value = parseInteger(arg.values.get(0), constrainPositive,
                constrainNotZero, this);
    }

    public int getInt() {
        return value;
    }
}
