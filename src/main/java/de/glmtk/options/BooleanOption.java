package de.glmtk.options;

public class BooleanOption extends Option {
    private boolean value = false;

    public BooleanOption(String shortopt,
                         String longopt,
                         String desc) {
        super(shortopt, longopt, desc);
    }

    public BooleanOption defaultValue(boolean defaultValue) {
        value = defaultValue;
        return this;
    }

    @Override
    protected void parse() throws OptionException {
        value = true;
    }

    public boolean getBoolean() {
        return value;
    }
}
