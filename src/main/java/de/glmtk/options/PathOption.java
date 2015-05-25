package de.glmtk.options;

import static java.nio.file.Files.exists;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.isReadable;
import static java.nio.file.Files.isRegularFile;
import static java.util.Objects.requireNonNull;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PathOption extends Option {
    public static final String DEFAULT_ARGNAME = "PATH";

    /* package */static Path parsePath(String pathString,
                                       boolean mayExist,
                                       boolean mustExist,
                                       boolean needFile,
                                       boolean needDirectory,
                                       Option option) throws OptionException {
        Path path;
        try {
            path = Paths.get(pathString);
        } catch (InvalidPathException e) {
            throw new OptionException("Option %s path could not be parsed as "
                    + "a path: '%s'. Reason: %s.", option, pathString,
                    e.getMessage());
        }

        if (!mayExist && !mustExist)
            return path;
        if (mayExist && !exists(path))
            return path;
        if (mustExist && !exists(path))
            throw new OptionException("Option %s path does not exist: '%s'.",
                    option, path);

        if (!isReadable(path))
            throw new OptionException("Option %s path does exist, "
                    + "but is not readable: '%s'.", option, path);
        if (needFile && !isRegularFile(path))
            throw new OptionException("Option %s path is required to be a "
                    + "file, but was not: '%s'.", option, path);
        else if (needDirectory && !isDirectory(path))
            throw new OptionException("Option %s path is required to be a "
                    + "directory, but was not: '%s'.", option, path);

        return path;
    }

    private String argname;
    private boolean mayExist = false;
    private boolean mustExist = false;
    private boolean needFile = false;
    private boolean needDirectory = false;
    private Path value = null;

    public PathOption(String shortopt,
                      String longopt,
                      String desc) {
        this(shortopt, longopt, desc, DEFAULT_ARGNAME);
    }

    public PathOption(String shortopt,
                      String longopt,
                      String desc,
                      String argname) {
        super(shortopt, longopt, desc);

        requireNonNull(argname);

        this.argname = argname;
    }

    public PathOption mayExist() {
        mayExist = true;
        checkConstraintsConflict();
        return this;
    }

    /**
     * Checks if path exists and is readable.
     */
    public PathOption mustExist() {
        mustExist = true;
        checkConstraintsConflict();
        return this;
    }

    public PathOption needFile() {
        needFile = true;
        checkConstraintsConflict();
        improveArgname();
        return this;
    }

    public PathOption needDirectory() {
        needDirectory = true;
        checkConstraintsConflict();
        improveArgname();
        return this;
    }

    private void checkConstraintsConflict() {
        if (needFile && needDirectory)
            throw new IllegalStateException(
                    "Conflict: both needFile() and needDirectory() active.");
        if (mayExist & mustExist)
            throw new IllegalStateException(
                    "Conflict: both mayExist() and mustExist() active.");
    }

    private void improveArgname() {
        if (argname.equals(DEFAULT_ARGNAME))
            if (needFile)
                argname = "FILE";
            else if (needDirectory)
                argname = "DIR";
    }

    public PathOption defaultValue(Path defaultValue) {
        value = defaultValue;
        return this;
    }

    @Override
    /* package */org.apache.commons.cli.Option createCommonsCliOption() {
        org.apache.commons.cli.Option commonsCliOption = new org.apache.commons.cli.Option(
                shortopt, longopt, true, desc);
        commonsCliOption.setArgName(argname);
        commonsCliOption.setArgs(1);
        return commonsCliOption;
    }

    @Override
    /* package */void parse(org.apache.commons.cli.Option commonsCliOption) throws OptionException {
        checkOnlyDefinedOnce();

        value = parsePath(commonsCliOption.getValue(), mayExist, mustExist,
                needFile, needDirectory, this);
    }

    public Path getPath() {
        return value;
    }
}
