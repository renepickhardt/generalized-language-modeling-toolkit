package de.glmtk.options;

public class BooleanOption extends Option {
    private boolean defaultValue = false;

    public BooleanOption(String shortopt,
                         String longopt,
                         String desc) {
        super(shortopt, longopt, desc);
    }

    public BooleanOption defaultValue(boolean defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public boolean getBoolean() {
        throw new UnsupportedOperationException();
    }
}
