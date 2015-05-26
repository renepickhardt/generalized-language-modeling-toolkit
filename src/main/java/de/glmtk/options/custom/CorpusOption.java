package de.glmtk.options.custom;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static de.glmtk.options.PathOption.parsePath;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.isReadable;
import static java.nio.file.Files.isRegularFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.google.common.collect.ImmutableList;

import de.glmtk.Constants;
import de.glmtk.options.Option;
import de.glmtk.options.OptionException;

public class CorpusOption extends Option {
    public static final String CORPUS_DEFAULT_ARGNAME = "CORPUS";
    public static final String WORKINGDIR_DEFAULT_ARGNAME = "WORKINGDIR";

    private Arg corpusArg = new Arg(CORPUS_DEFAULT_ARGNAME, 1);
    private Arg workingDirArg = new Arg(WORKINGDIR_DEFAULT_ARGNAME, MAX_ONE);
    private String suffix = Constants.WORKING_DIR_SUFFIX;
    private Path corpus = null;
    private Path workingDir = null;

    public CorpusOption(String shortopt,
                        String longopt,
                        String desc) {
        super(shortopt, longopt, desc);
    }

    public CorpusOption corpusArgName(String argName) {
        checkNotNull(argName);
        checkArgument(!argName.isEmpty());
        corpusArg.name = argName;
        return this;
    }

    public CorpusOption workingDirArgName(String argName) {
        checkNotNull(argName);
        checkArgument(!argName.isEmpty());
        workingDirArg.name = argName;
        return this;
    }

    public CorpusOption defaultValue(Path defaultValue) {
        corpus = defaultValue;
        return this;
    }

    public CorpusOption suffix(String suffix) {
        checkNotNull(suffix);
        checkArgument(!suffix.isEmpty());
        this.suffix = suffix;
        return this;
    }

    @Override
    protected List<Arg> arguments() {
        return ImmutableList.of(corpusArg, workingDirArg);
    }

    @Override
    protected void parse() throws OptionException {
        corpus = parsePath(corpusArg.value, this);
        if (workingDirArg.value != null)
            workingDir = parsePath(workingDirArg.value, this);

        if (isDirectory(corpus)) {
            if (workingDir != null)
                throw new OptionException("%s: Can't explicitly specify "
                        + "working directory '%s' if given corpus is already "
                        + "a directory.", this, workingDir, corpus);
            workingDir = corpus;
            corpus = checkWorkingDirFile(Constants.TRAINING_FILE_NAME);
            checkWorkingDirFile(Constants.STATUS_FILE_NAME);
        } else {
            if (workingDir == null)
                workingDir = Paths.get(corpus + suffix);
            if (exists(workingDir) && !isDirectory(workingDir))
                throw new OptionException(
                        "%s: Working directory '%s' exists, but "
                                + "is not a directory.", this, workingDir);
            if (exists(workingDir) && !isReadable(workingDir))
                throw new OptionException(
                        "%s: Working directory '%s' exists, but "
                                + "is not readable.", this, workingDir);
        }
    }

    private Path checkWorkingDirFile(String filename) throws OptionException {
        Path file = workingDir.resolve(filename);
        if (!exists(file) || !isReadable(file) || !isRegularFile(file))
            throw new OptionException("%s: %s file '%s' does not exist, is "
                    + "not readable, or not a regular file.", this, filename,
                    file);
        return file;
    }

    public Path getCorpus() {
        return corpus;
    }

    public Path getWorkingDir() {
        return workingDir;
    }
}
