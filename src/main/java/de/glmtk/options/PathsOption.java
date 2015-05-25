package de.glmtk.options;

import static de.glmtk.options.PathOption.parsePath;
import static de.glmtk.util.revamp.ListUtils.list;
import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.util.List;

public class PathsOption extends Option {
    public static final String DEFAULT_ARGNAME = PathOption.DEFAULT_ARGNAME;

    private String argname;
    private boolean mayExist = false;
    private boolean mustExist = false;
    private boolean needFiles = false;
    private boolean needDirectories = false;
    private List<Path> value = list();
    private boolean explicitDefault = false;

    public PathsOption(String shortopt,
                       String longopt,
                       String desc) {
        this(shortopt, longopt, desc, DEFAULT_ARGNAME);
    }

    public PathsOption(String shortopt,
                       String longopt,
                       String desc,
                       String argname) {
        super(shortopt, longopt, desc);

        requireNonNull(argname);

        this.argname = argname;
    }

    public PathsOption mayExist() {
        mayExist = true;
        checkConstraintsConflict();
        return this;
    }

    /**
     * Checks if paths exists and is readable.
     */
    public PathsOption mustExist() {
        mustExist = true;
        checkConstraintsConflict();
        return this;
    }

    public PathsOption needFiles() {
        needFiles = true;
        checkConstraintsConflict();
        improveArgname();
        return this;
    }

    public PathsOption needDirectories() {
        needDirectories = true;
        checkConstraintsConflict();
        improveArgname();
        return this;
    }

    private void checkConstraintsConflict() {
        if (needFiles && needDirectories)
            throw new IllegalStateException(
                    "Conflict: both needFiles() and needDirectories() active.");
        if (mayExist & mustExist)
            throw new IllegalStateException(
                    "Conflict: both mayExist() and mustExist() active.");
    }

    private void improveArgname() {
        if (argname.equals(DEFAULT_ARGNAME))
            if (needFiles)
                argname = "FILE";
            else if (needDirectories)
                argname = "DIR";
    }

    public PathsOption defaultValue(List<Path> defaultValue) {
        value = defaultValue;
        explicitDefault = true;
        return this;
    }

    @Override
    /* package */org.apache.commons.cli.Option createCommonsCliOption() {
        org.apache.commons.cli.Option commonsCliOption = new org.apache.commons.cli.Option(
                shortopt, longopt, true, desc);
        commonsCliOption.setArgName(argname + MULTIPLE_ARG_SUFFIX);
        commonsCliOption.setArgs(org.apache.commons.cli.Option.UNLIMITED_VALUES);
        return commonsCliOption;
    }

    @Override
    /* package */void parse(org.apache.commons.cli.Option commonsCliOption) throws OptionException {
        if (explicitDefault) {
            explicitDefault = false;
            value = list();
        }

        for (String pathString : commonsCliOption.getValues())
            value.add(parsePath(pathString, mayExist, mustExist, needFiles,
                    needDirectories, this));
    }

    public List<Path> getPaths() {
        return value;
    }
}
