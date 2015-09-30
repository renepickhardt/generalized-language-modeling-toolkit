package de.glmtk.options;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.isReadable;
import static java.nio.file.Files.isRegularFile;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.google.common.collect.ImmutableList;


public class PathOption extends Option {
    public static final String DEFAULT_ARGNAME = "PATH";

    public static Path parsePath(String pathString,
                                 Option option) throws OptionException {
        return parsePath(pathString, false, false, false, false, option);
    }

    public static Path parsePath(String pathString,
                                 boolean requireMayExist,
                                 boolean requireMustExist,
                                 boolean requireFile,
                                 boolean requireDirectory,
                                 Option option) throws OptionException {
        checkNotNull(pathString);
        checkNotNull(option);

        Path path;
        try {
            path = Paths.get(pathString);
        } catch (InvalidPathException e) {
            throw new OptionException(
                "%s path could not be parsed as " + "a path: '%s'. Reason: %s.",
                option, pathString, e.getMessage());
        }

        if (!requireMayExist && !requireMustExist) {
            return path;
        }
        if (requireMayExist && !exists(path)) {
            return path;
        }
        if (requireMustExist && !exists(path)) {
            throw new OptionException("%s path does not exist: '%s'.", option,
                path);
        }

        if (!isReadable(path)) {
            throw new OptionException(
                "%s path does exist, " + "but is not readable: '%s'.", option,
                path);
        }
        if (requireFile && !isRegularFile(path)) {
            throw new OptionException(
                "%s path is required to be a " + "file, but was not: '%s'.",
                option, path);
        } else if (requireDirectory && !isDirectory(path)) {
            throw new OptionException("%s path is required to be a "
                + "directory, but was not: '%s'.", option, path);
        }

        return path;
    }

    private Arg arg = new Arg(DEFAULT_ARGNAME, 1);
    private boolean requireMayExist = false;
    private boolean requireMustExist = false;
    private boolean requireFile = false;
    private boolean requireDirectory = false;
    private Path value = null;

    public PathOption(String shortopt,
                      String longopt,
                      String desc) {
        super(shortopt, longopt, desc);
    }

    public PathOption argName(String argName) {
        checkNotNull(argName);
        checkArgument(!argName.isEmpty());
        arg.name = argName;
        return this;
    }

    public PathOption requireMayExist() {
        requireMayExist = true;
        checkrequiretsConflict();
        return this;
    }

    /**
     * Checks if path exists and is readable.
     */
    public PathOption requireMustExist() {
        requireMustExist = true;
        checkrequiretsConflict();
        return this;
    }

    public PathOption requireFile() {
        requireFile = true;
        checkrequiretsConflict();
        improveArgName();
        return this;
    }

    public PathOption requireDirectory() {
        requireDirectory = true;
        checkrequiretsConflict();
        improveArgName();
        return this;
    }

    private void checkrequiretsConflict() {
        if (requireFile && requireDirectory) {
            throw new IllegalStateException(
                "Conflict: both needFile() and needDirectory() active.");
        }
        if (requireMayExist & requireMustExist) {
            throw new IllegalStateException(
                "Conflict: both mayExist() and mustExist() active.");
        }
    }

    private void improveArgName() {
        if (arg.name.equals(DEFAULT_ARGNAME)) {
            if (requireFile) {
                arg.name = "FILE";
            } else if (requireDirectory) {
                arg.name = "DIR";
            }
        }
    }

    public PathOption defaultValue(Path defaultValue) {
        value = defaultValue;
        return this;
    }

    @Override
    protected List<Arg> arguments() {
        return ImmutableList.of(arg);
    }

    @Override
    protected void parse() throws OptionException {
        value = parsePath(arg.value, requireMayExist, requireMustExist,
            requireFile, requireDirectory, this);
    }

    public Path getPath() {
        return value;
    }
}
