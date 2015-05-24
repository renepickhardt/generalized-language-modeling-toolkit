package de.glmtk.options;

import static de.glmtk.util.revamp.ListUtils.list;
import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.util.List;

public class PathsOption extends Option {
    public static final String DEFAULT_ARGNAME = PathOption.DEFAULT_ARGNAME;

    private String argname;
    private boolean mustExist = false;
    private boolean mustBeFiles = false;
    private boolean mustBeDiretories = false;
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

    /**
     * Checks if paths exists and is readable.
     */
    public PathsOption mustExist() {
        mustExist = true;
        return this;
    }

    public PathsOption mustBeFiles() {
        if (mustBeDiretories)
            throw new IllegalStateException(
                    "Conflict: mustBeFile() and mustBeDirectory().");
        mustBeFiles = true;
        if (argname.equals(DEFAULT_ARGNAME))
            argname = "FILE";
        return this;
    }

    public PathsOption mustBeDirectories() {
        if (mustBeFiles)
            throw new IllegalStateException(
                    "Conflict: mustBeFile() and mustBeDirectory().");
        mustBeDiretories = true;
        if (argname.equals(DEFAULT_ARGNAME))
            argname = "DIR";
        return this;
    }

    public PathsOption defaultValue(List<Path> defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public List<Path> getPaths() {
        throw new UnsupportedOperationException();
    }
}
