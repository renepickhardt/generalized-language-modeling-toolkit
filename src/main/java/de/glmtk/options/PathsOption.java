package de.glmtk.options;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static de.glmtk.options.PathOption.parsePath;

import java.nio.file.Path;
import java.util.List;

import com.google.common.collect.ImmutableList;

public class PathsOption extends Option {
    public static final String DEFAULT_ARGNAME = PathOption.DEFAULT_ARGNAME;

    private Arg arg = new Arg(DEFAULT_ARGNAME, GREATER_ONE);
    private boolean requireMayExist = false;
    private boolean requireMustExist = false;
    private boolean requireFiles = false;
    private boolean requireDirectories = false;
    private List<Path> value = newArrayList();

    public PathsOption(String shortopt,
                       String longopt,
                       String desc) {
        super(shortopt, longopt, desc);
        mayBeGivenRepeatedly = true;
    }

    public PathsOption argName(String argName) {
        checkNotNull(argName);
        checkArgument(!argName.isEmpty());
        arg.name = argName;
        return this;
    }

    public PathsOption requireMayExist() {
        requireMayExist = true;
        checkrequiretsConflict();
        return this;
    }

    /**
     * Checks if paths exists and is readable.
     */
    public PathsOption requireMustExist() {
        requireMustExist = true;
        checkrequiretsConflict();
        return this;
    }

    public PathsOption requireFiles() {
        requireFiles = true;
        checkrequiretsConflict();
        improveArgName();
        return this;
    }

    public PathsOption needDirectories() {
        requireDirectories = true;
        checkrequiretsConflict();
        improveArgName();
        return this;
    }

    private void checkrequiretsConflict() {
        if (requireFiles && requireDirectories)
            throw new IllegalStateException(
                    "Conflict: both needFiles() and needDirectories() active.");
        if (requireMayExist & requireMustExist)
            throw new IllegalStateException(
                    "Conflict: both mayExist() and mustExist() active.");
    }

    private void improveArgName() {
        if (arg.name.equals(DEFAULT_ARGNAME))
            if (requireFiles)
                arg.name = "FILE";
            else if (requireDirectories)
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
            value.add(parsePath(pathString, requireMayExist, requireMustExist,
                    requireFiles, requireDirectories, this));
    }

    public List<Path> getPaths() {
        return value;
    }
}
