package de.glmtk.options;

import static java.nio.file.Files.exists;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.isReadable;
import static java.nio.file.Files.isRegularFile;
import static java.util.Objects.requireNonNull;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.google.common.collect.ImmutableList;

public class PathOption extends Option {
    public static final String DEFAULT_ARGNAME = "PATH";

    public static Path parsePath(String pathString,
                                 boolean constrainMayExist,
                                 boolean constrainMustExist,
                                 boolean constrainFile,
                                 boolean constrainDirectory,
                                 Option option) throws OptionException {
        requireNonNull(pathString);
        requireNonNull(option);

        Path path;
        try {
            path = Paths.get(pathString);
        } catch (InvalidPathException e) {
            throw new OptionException("Option %s path could not be parsed as "
                    + "a path: '%s'. Reason: %s.", option, pathString,
                    e.getMessage());
        }

        if (!constrainMayExist && !constrainMustExist)
            return path;
        if (constrainMayExist && !exists(path))
            return path;
        if (constrainMustExist && !exists(path))
            throw new OptionException("Option %s path does not exist: '%s'.",
                    option, path);

        if (!isReadable(path))
            throw new OptionException("Option %s path does exist, "
                    + "but is not readable: '%s'.", option, path);
        if (constrainFile && !isRegularFile(path))
            throw new OptionException("Option %s path is required to be a "
                    + "file, but was not: '%s'.", option, path);
        else if (constrainDirectory && !isDirectory(path))
            throw new OptionException("Option %s path is required to be a "
                    + "directory, but was not: '%s'.", option, path);

        return path;
    }

    private Arg arg = new Arg(DEFAULT_ARGNAME, 1);
    private boolean constrainMayExist = false;
    private boolean constrainMustExist = false;
    private boolean constrainFile = false;
    private boolean constrainDirectory = false;
    private Path value = null;

    public PathOption(String shortopt,
                      String longopt,
                      String desc) {
        super(shortopt, longopt, desc);
    }

    public PathOption argName(String argName) {
        requireNonNull(argName);
        arg.name = argName;
        return this;
    }

    public PathOption constrainMayExist() {
        constrainMayExist = true;
        checkConstraintsConflict();
        return this;
    }

    /**
     * Checks if path exists and is readable.
     */
    public PathOption constrainMustExist() {
        constrainMustExist = true;
        checkConstraintsConflict();
        return this;
    }

    public PathOption constrainFile() {
        constrainFile = true;
        checkConstraintsConflict();
        improveArgName();
        return this;
    }

    public PathOption constrainDirectory() {
        constrainDirectory = true;
        checkConstraintsConflict();
        improveArgName();
        return this;
    }

    private void checkConstraintsConflict() {
        if (constrainFile && constrainDirectory)
            throw new IllegalStateException(
                    "Conflict: both needFile() and needDirectory() active.");
        if (constrainMayExist & constrainMustExist)
            throw new IllegalStateException(
                    "Conflict: both mayExist() and mustExist() active.");
    }

    private void improveArgName() {
        if (arg.name.equals(DEFAULT_ARGNAME))
            if (constrainFile)
                arg.name = "FILE";
            else if (constrainDirectory)
                arg.name = "DIR";
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
        value = parsePath(arg.values.get(0), constrainMayExist,
                constrainMustExist, constrainFile, constrainDirectory, this);
    }

    public Path getPath() {
        return value;
    }
}
