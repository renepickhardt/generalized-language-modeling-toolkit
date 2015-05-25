package de.glmtk.options;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import java.util.List;

import com.google.common.collect.ImmutableList;

public abstract class Option {
    protected static class Arg {
        public String name;
        public int count;
        public String explanation = null;
        public List<String> values = null;

        public Arg(String name,
                   int count) {
            this.name = name;
            this.count = count;
        }

        public Arg(String name,
                   int count,
                   String explanation) {
            this.name = name;
            this.count = count;
            this.explanation = explanation;
        }
    }

    protected static final int MORE_THAN_ONE = -1;

    /* package */String shortopt;
    /* package */String longopt;
    /* package */String desc;
    protected boolean given = false;
    protected boolean mayBeGivenRepeatedly = false;

    /**
     * @param shortopt
     *            May be {@code null}.
     */
    public Option(String shortopt,
                  String longopt,
                  String desc) {
        requireNonNull(longopt);
        requireNonNull(desc);

        this.shortopt = shortopt;
        this.longopt = longopt;
        this.desc = desc;
    }

    @Override
    public String toString() {
        if (shortopt == null)
            return format("--%s", longopt);
        return format("-%s (--%s)", shortopt, longopt);
    }

    protected List<Arg> arguments() {
        return ImmutableList.of();
    }

    /* package */final void runParse() throws OptionException {
        if (given && !mayBeGivenRepeatedly)
            throw new OptionException("Option %s may only be given once.", this);

        parse();
        given = true;
    }

    protected abstract void parse() throws OptionException;

    public boolean wasGiven() {
        return given;
    }
}
