package de.glmtk.options;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;

public class PathOption extends Option {
    public static final String DEFAULT_ARGNAME = "PATH";

    private String argname;
    private boolean mustExist = false;
    private boolean mustBeFile = false;
    private boolean mustBeDiretory = false;
    private Path defaultValue = null;

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

    /**
     * Checks if path exists and is readable.
     */
    public PathOption mustExist() {
        mustExist = true;
        return this;
    }

    public PathOption mustBeFile() {
        if (mustBeDiretory)
            throw new IllegalStateException(
                    "Conflict: mustBeFile() and mustBeDirectory().");
        mustBeFile = true;
        if (argname.equals(DEFAULT_ARGNAME))
            argname = "FILE";
        return this;
    }

    public PathOption mustBeDirectory() {
        if (mustBeFile)
            throw new IllegalStateException(
                    "Conflict: mustBeFile() and mustBeDirectory().");
        mustBeDiretory = true;
        if (argname.equals(DEFAULT_ARGNAME))
            argname = "DIR";
        return this;
    }

    public PathOption defaultValue(Path defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public Path getPath() {
        throw new UnsupportedOperationException();
    }
}
