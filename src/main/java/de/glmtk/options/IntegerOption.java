package de.glmtk.options;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import com.google.common.collect.ImmutableList;

public class IntegerOption extends Option {
    public static final String DEFAULT_ARGNAME = "INT";

    public static final int parseInteger(String integerString,
                                         boolean requirePositive,
                                         boolean requireNotZero,
                                         Option option) throws OptionException {
        checkNotNull(integerString);
        checkNotNull(option);

        int value;
        try {
            value = Integer.parseInt(integerString);
        } catch (NumberFormatException e) {
            throw new OptionException("%s could not be parsed as an "
                    + "integer: '%s'. Reason: %s.", option, integerString,
                    e.getMessage());
        }

        if (requirePositive && value < 0)
            throw new OptionException(
                    "%s must not be negative, got '%d' instead.", option, value);

        if (requireNotZero && value == 0)
            throw new OptionException("%s must not be zero.", option);

        return value;
    }

    private Arg arg = new Arg(DEFAULT_ARGNAME, 1);
    private boolean requirePositive = false;
    private boolean requireNotZero = false;
    private int value = 0;

    public IntegerOption(String shortopt,
                         String longopt,
                         String desc) {
        super(shortopt, longopt, desc);
    }

    public IntegerOption argName(String argName) {
        checkNotNull(argName);
        checkArgument(!argName.isEmpty());
        arg.name = argName;
        return this;
    }

    /**
     * Allows zero and greater integers.
     */
    public IntegerOption requirePositive() {
        requirePositive = true;
        improveArgName();
        return this;
    }

    public IntegerOption requireNotZero() {
        requireNotZero = true;
        improveArgName();
        return this;
    }

    private void improveArgName() {
        if (arg.name.equals(DEFAULT_ARGNAME))
            if (requirePositive && requireNotZero)
                arg.name += ">0";
            else if (requirePositive)
                arg.name += ">=0";
            else if (requireNotZero)
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
        value = parseInteger(arg.value, requirePositive, requireNotZero, this);
    }

    public int getInt() {
        return value;
    }
}
