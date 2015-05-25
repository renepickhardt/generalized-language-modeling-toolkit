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
    /* package */org.apache.commons.cli.Option createCommonsCliOption() {
        org.apache.commons.cli.Option commonsCliOption = new org.apache.commons.cli.Option(
                shortopt, longopt, false, desc);
        return commonsCliOption;
    }

    @Override
    protected void handleParse(org.apache.commons.cli.Option commonsCliOption) throws OptionException {
        checkOnlyDefinedOnce();
        value = true;
    }

    public boolean getBoolean() {
        return value;
    }

}
