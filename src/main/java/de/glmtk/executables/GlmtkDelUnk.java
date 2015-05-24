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
import java.util.Arrays;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.cli.Option;

import de.glmtk.Constants;
import de.glmtk.Glmtk;
import de.glmtk.cache.Cache;
import de.glmtk.cache.CacheSpecification;
import de.glmtk.cache.CacheSpecification.CacheImplementation;
import de.glmtk.common.NGram;
import de.glmtk.common.Patterns;
import de.glmtk.exceptions.CliArgumentException;
import de.glmtk.logging.Logger;
import de.glmtk.util.NioUtils;
import de.glmtk.util.StringUtils;

public class GlmtkDelUnk extends Executable {
    private static final Logger LOGGER = Logger.get(GlmtkDelUnk.class);

    private static final Option OPTION_HELP;
    private static final Option OPTION_VERSION;

    private static final List<Option> OPTIONS;

    static {
        OPTION_HELP = new Option(OPTION_HELP_SHORT, OPTION_HELP_LONG, false,
                "Print this message.");

        OPTION_VERSION = new Option(OPTION_VERSION_SHORT, OPTION_VERSION_LONG,
                false, "Print the version infromation and exit.");

        OPTIONS = Arrays.asList(OPTION_HELP, OPTION_VERSION);
    }

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
    protected List<Option> getOptions() {
        return OPTIONS;
    }

    @Override
    protected String getHelpHeader() {
        try (Formatter f = new Formatter()) {
            f.format("%s <INPUT> [<OPTION...>]%n", getExecutableName());
            f.format("Takes input on stdin. Outputs all lines not containing unkown words.%n");

            f.format("%nMandatory arguments to long options are mandatory for short options too.%n");

            return f.toString();
        }
    }

    @Override
    protected String getHelpFooter() {
        try (Formatter f = new Formatter()) {
            f.format("%nFor more information, see:%n");
            f.format("https://github.com/renepickhardt/generalized-language-modeling-toolkit/%n");

            return f.toString();
        }
    }

    @Override
    protected void parseArguments(String[] args) throws Exception {
        super.parseArguments(args);

        corpus = parseInputArg();
        parseFlags();

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

    private void parseFlags() {
        @SuppressWarnings("unchecked")
        Iterator<Option> iter = line.iterator();
        while (iter.hasNext()) {
            Option option = iter.next();

            throw new CliArgumentException(String.format(
                    "Unexpected option: '%s'.", option));
        }
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
