package de.glmtk.options;

import static de.glmtk.util.revamp.ListUtils.list;

import java.util.List;

public class OptionManager {
    private List<Option> options = list();

    public OptionManager register(Option... options) {
        for (Option option : options)
            this.options.add(option);
        return this;
    }

    public String helpString() {
        throw new UnsupportedOperationException();
    }

    public void parse(String[] args) {
        throw new UnsupportedOperationException();
    }
}
