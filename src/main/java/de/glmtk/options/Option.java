package de.glmtk.options;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

import java.util.List;

import com.google.common.collect.ImmutableList;

import de.glmtk.exceptions.SwitchCaseNotImplementedException;


public abstract class Option {
    protected static class Arg {
        public String name;
        public int count;
        public String explanation = null;
        public List<String> values = null;
        /** Convenience for {@code values.get(0)}. */
        public String value;

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

    /* package */static enum Type {
        OPTION, INPUT_ARG
    }

    protected static final int GREATER_ONE = -1;
    protected static final int MAX_ONE = -2;

    /* package */Type type = Type.OPTION;
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
        checkNotNull(longopt);
        checkArgument(!longopt.isEmpty());
        checkNotNull(desc);

        this.shortopt = shortopt;
        this.longopt = longopt;
        this.desc = desc;
    }

    @Override
    public String toString() {
        switch (type) {
            case OPTION:
                if (shortopt == null) {
                    return format("Option --%s", longopt);
                }
                return format("Option -%s (--%s)", shortopt, longopt);

            case INPUT_ARG:
                return format("Input argument '%s'", longopt);

            default:
                throw new SwitchCaseNotImplementedException();
        }
    }

    protected List<Arg> arguments() {
        return ImmutableList.of();
    }

    /* package */final void runParse() throws OptionException {
        if (given && !mayBeGivenRepeatedly) {
            throw new OptionException("%s may only be given once.", this);
        }

        parse();
        given = true;
    }

    protected abstract void parse() throws OptionException;

    public boolean wasGiven() {
        return given;
    }
}
