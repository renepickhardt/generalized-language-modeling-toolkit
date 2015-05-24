package de.glmtk.options;

import static java.util.Objects.requireNonNull;

public class IntegerOption extends Option {
    public static final String DEFAULT_ARGNAME = "INT";

    private String argname;
    private int defaultValue = 0;
    private boolean mustBePositive = false;
    private boolean mustNotBeZero = false;

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
        this.defaultValue = defaultValue;
        return this;
    }

    public int getInt() {
        throw new UnsupportedOperationException();
    }
}
