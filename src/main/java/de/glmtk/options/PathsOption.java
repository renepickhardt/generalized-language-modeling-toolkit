package de.glmtk.options;

import static com.google.common.collect.Lists.newArrayList;
import static de.glmtk.options.PathOption.parsePath;
import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.util.List;

import com.google.common.collect.ImmutableList;

public class PathsOption extends Option {
    public static final String DEFAULT_ARGNAME = PathOption.DEFAULT_ARGNAME;

    private Arg arg = new Arg(DEFAULT_ARGNAME, MORE_THAN_ONE);
    private boolean constrainMayExist = false;
    private boolean constrainMustExist = false;
    private boolean constrainFiles = false;
    private boolean constrainDirectories = false;
    private List<Path> value = newArrayList();

    public PathsOption(String shortopt,
                       String longopt,
                       String desc) {
        super(shortopt, longopt, desc);
        mayBeGivenRepeatedly = true;
    }

    public PathsOption argName(String argName) {
        requireNonNull(argName);
        arg.name = argName;
        return this;
    }

    public PathsOption constrainMayExist() {
        constrainMayExist = true;
        checkConstraintsConflict();
        return this;
    }

    /**
     * Checks if paths exists and is readable.
     */
    public PathsOption constrainMustExist() {
        constrainMustExist = true;
        checkConstraintsConflict();
        return this;
    }

    public PathsOption constrainFiles() {
        constrainFiles = true;
        checkConstraintsConflict();
        improveArgName();
        return this;
    }

    public PathsOption needDirectories() {
        constrainDirectories = true;
        checkConstraintsConflict();
        improveArgName();
        return this;
    }

    private void checkConstraintsConflict() {
        if (constrainFiles && constrainDirectories)
            throw new IllegalStateException(
                    "Conflict: both needFiles() and needDirectories() active.");
        if (constrainMayExist & constrainMustExist)
            throw new IllegalStateException(
                    "Conflict: both mayExist() and mustExist() active.");
    }

    private void improveArgName() {
        if (arg.name.equals(DEFAULT_ARGNAME))
            if (constrainFiles)
                arg.name = "FILE";
            else if (constrainDirectories)
                arg.name = "DIR";
    }

    public PathsOption defaultValue(List<Path> defaultValue) {
        value = defaultValue;
        return this;
    }

    @Override
    protected List<Arg> arguments() {
        return ImmutableList.of(arg);
    }

    @Override
    protected void parse() throws OptionException {
        if (!given)
            value = newArrayList();

        for (String pathString : arg.values)
            value.add(parsePath(pathString, constrainMayExist,
                    constrainMustExist, constrainFiles, constrainDirectories,
                    this));
    }

    public List<Path> getPaths() {
        return value;
    }
}
