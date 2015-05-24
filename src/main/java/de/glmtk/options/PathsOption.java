package de.glmtk.options;

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
    private boolean needDiretories = false;
    private List<Path> defaultValue = list();

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
        needDiretories = true;
        checkConstraintsConflict();
        improveArgname();
        return this;
    }

    private void checkConstraintsConflict() {
        if (needFiles && needDiretories)
            throw new IllegalStateException(
                    "Conflict: both needFile() and needDirectory() active.");
        if (mayExist & mustExist)
            throw new IllegalStateException(
                    "Conflict: both mayExist() and mustExist() active.");
    }

    private void improveArgname() {
        if (argname.equals(DEFAULT_ARGNAME))
            if (needFiles)
                argname = "FILE";
            else if (needDiretories)
                argname = "DIR";
    }

    public PathsOption defaultValue(List<Path> defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public List<Path> getPaths() {
        throw new UnsupportedOperationException();
    }
}
