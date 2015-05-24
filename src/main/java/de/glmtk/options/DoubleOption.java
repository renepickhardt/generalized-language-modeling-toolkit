package de.glmtk.options;

import static java.util.Objects.requireNonNull;

public class DoubleOption extends Option {
    public static final String DEFAULT_ARGNAME = "FLOAT";

    private String argname;
    private double defaultValue = 0.0;
    private boolean mustBeProb = false;

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
        this.defaultValue = defaultValue;
        return this;
    }

    public double getDouble() {
        throw new UnsupportedOperationException();
    }
}
