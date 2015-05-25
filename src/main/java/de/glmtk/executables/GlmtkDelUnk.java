package de.glmtk.executables;

import static de.glmtk.util.NioUtils.CheckFile.EXISTS;
import static de.glmtk.util.NioUtils.CheckFile.IS_DIRECTORY;
import static de.glmtk.util.NioUtils.CheckFile.IS_READABLE;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import de.glmtk.Constants;
import de.glmtk.Glmtk;
import de.glmtk.cache.Cache;
import de.glmtk.cache.CacheSpecification;
import de.glmtk.cache.CacheSpecification.CacheImplementation;
import de.glmtk.common.NGram;
import de.glmtk.common.Patterns;
import de.glmtk.logging.Logger;
import de.glmtk.util.NioUtils;
import de.glmtk.util.StringUtils;

public class GlmtkDelUnk extends Executable {
    private static final Logger LOGGER = Logger.get(GlmtkDelUnk.class);

    public static void main(String[] args) {
        new GlmtkDelUnk().run(args);
    }

    private Path corpus = null;
    private Path workingDir = null;

    @Override
    protected String getExecutableName() {
        return "glmtk-delunk";
    }

    @Override
    protected void registerOptions() {
    }

    @Override
    protected String getHelpHeader() {
        return "Takes input on stdin. Outputs all lines not containing unkown words.";
    }

    @Override
    protected String getHelpFooter() {
        return null;
    }

    @Override
    protected void parseOptions(String[] args) throws Exception {
        super.parseOptions(args);

        corpus = parseInputArg();

        if (NioUtils.checkFile(corpus, IS_DIRECTORY))
            workingDir = corpus;
        else
            workingDir = Paths.get(corpus + Constants.WORKING_DIR_SUFFIX);
        corpus = getWorkingDirFile(workingDir, Constants.TRAINING_FILE_NAME);
        if (!NioUtils.checkFile(workingDir, IS_DIRECTORY))
            throw new IOException(String.format(
                    "Working directory '%s' is not a directory.", workingDir));
        if (!NioUtils.checkFile(workingDir, EXISTS, IS_READABLE))
            throw new IOException(
                    String.format(
                            "Working directory '%s' does not exist or is not readable.",
                            workingDir));
    }

    @Override
    protected void exec() throws Exception {
        logFields();

        Glmtk glmtk = new Glmtk(config, corpus, workingDir);

        CacheSpecification chacheSpec = new CacheSpecification();
        chacheSpec.withCounts(Patterns.getMany("1")).withCacheImplementation(
                CacheImplementation.HASH_MAP).withProgress();
        Cache cache = chacheSpec.build(glmtk.getPaths());

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                System.in, Constants.CHARSET));
                OutputStreamWriter writer = new OutputStreamWriter(System.out,
                        Constants.CHARSET)) {
            String line;
            while ((line = reader.readLine()) != null) {
                List<String> words = StringUtils.split(line.trim(), ' ');

                boolean allWordsSeen = true;
                for (String word : words) {
                    NGram ngram = new NGram(word);
                    if (cache.getCount(ngram) == 0) {
                        allWordsSeen = false;
                        break;
                    }
                }

                if (allWordsSeen)
                    writer.append(line).append('\n');
                writer.flush();
            }
        }
    }

    private void logFields() {
        LOGGER.debug("%s %s", getExecutableName(), StringUtils.repeat("-",
                80 - getExecutableName().length()));
        LOGGER.debug("Corpus:     %s", corpus);
        LOGGER.debug("WorkingDir: %s", workingDir);
    }
}
