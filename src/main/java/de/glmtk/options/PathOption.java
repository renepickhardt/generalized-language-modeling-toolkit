package de.glmtk.options;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;

public class PathOption extends Option {
    public static final String DEFAULT_ARGNAME = "PATH";

    private String argname;
    private boolean mayExist = false;
    private boolean mustExist = false;
    private boolean needFile = false;
    private boolean needDiretory = false;
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
        needDiretory = true;
        checkConstraintsConflict();
        improveArgname();
        return this;
    }

    private void checkConstraintsConflict() {
        if (needFile && needDiretory)
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
            else if (needDiretory)
                argname = "DIR";
    }

    public PathOption defaultValue(Path defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public Path getPath() {
        throw new UnsupportedOperationException();
    }
}
