package de.glmtk.executables;

import static de.glmtk.utils.NioUtils.CheckFile.EXISTS;
import static de.glmtk.utils.NioUtils.CheckFile.IS_DIRECTORY;
import static de.glmtk.utils.NioUtils.CheckFile.IS_NO_DIRECTORY;
import static de.glmtk.utils.NioUtils.CheckFile.IS_READABLE;
import static de.glmtk.utils.NioUtils.CheckFile.IS_REGULAR_FILE;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.Option;

import de.glmtk.Glmtk;
import de.glmtk.Model;
import de.glmtk.Termination;
import de.glmtk.counting.CountCache;
import de.glmtk.querying.ProbMode;
import de.glmtk.querying.estimator.Estimator;
import de.glmtk.utils.LogUtils;
import de.glmtk.utils.NioUtils;
import de.glmtk.utils.Pattern;
import de.glmtk.utils.Patterns;
import de.glmtk.utils.StatisticalNumberHelper;
import de.glmtk.utils.StringUtils;

public class GlmtkExecutable extends Executable {

    // TODO: API to count all patterns.

    private static final Option OPTION_HELP;

    private static final Option OPTION_VERSION;

    private static final Option OPTION_WORKINGDIR;

    private static final Option OPTION_MODELSIZE;

    private static final Option OPTION_MODEL;

    private static final Option OPTION_TESTING;

    private static List<Option> options;
    static {
        //@formatter:off
        OPTION_HELP       = new Option(OPTION_HELP_SHORT,    OPTION_HELP_LONG,    false, "Print this message.");
        OPTION_VERSION    = new Option(OPTION_VERSION_SHORT, OPTION_VERSION_LONG, false, "Print the version information and exit.");
        OPTION_WORKINGDIR = new Option("w", "workingdir", true, "Working directory.");
        OPTION_WORKINGDIR.setArgName("WORKINGDIR");
        StringBuilder modelOptDesc = new StringBuilder();
        for (Model model : Model.values()) {
            String abbreviation = model.getAbbreviation();
            modelOptDesc.append(abbreviation);
            modelOptDesc.append(StringUtils.repeat(" ", 5 - abbreviation.length()));
            modelOptDesc.append("- ");
            modelOptDesc.append(model.getName());
            modelOptDesc.append(".\n");
        }
        OPTION_MODELSIZE  = new Option("n", "modelsize",  true, "MODELSIZE");
        OPTION_MODELSIZE.setArgName("MODELSIZE");
        OPTION_MODEL      = new Option("m", "model",      true, "Can be specified multiple times.\n" + modelOptDesc.toString());
        OPTION_MODEL.setArgName("MODEL");
        OPTION_MODEL.setArgs(Option.UNLIMITED_VALUES);
        OPTION_TESTING    = new Option("t", "testing",    true, "File to take testing sequences for probability and entropy from (can be specified multiple times).");
        OPTION_TESTING.setArgName("TESTING...");
        OPTION_TESTING.setArgs(Option.UNLIMITED_VALUES);
        //@formatter:on
        options =
                Arrays.asList(OPTION_HELP, OPTION_VERSION, OPTION_WORKINGDIR,
                        OPTION_MODELSIZE, OPTION_MODEL, OPTION_TESTING);
    }

    private Path corpus = null;

    private Path workingDir = null;

    private int modelSize = 5;

    private Set<Model> models = new HashSet<Model>();

    private Set<Path> testingFiles = new HashSet<Path>();

    public static void main(String[] args) {
        new GlmtkExecutable().run(args);
    }

    @Override
    protected List<Option> getOptions() {
        return options;
    }

    @Override
    protected String getUsage() {
        return "glmtk <INPUT> [OPTION]...";
    }

    @Override
    protected void parseArguments(String[] args) {
        super.parseArguments(args);

        if (line.getArgs() == null || line.getArgs().length == 0) {
            throw new Termination("Missing input.\n"
                    + "Try 'glmtk --help' for more information.");
        }

        Path inputArg = Paths.get(line.getArgs()[0]);
        if (!NioUtils.checkFile(inputArg, EXISTS, IS_READABLE)) {
            throw new Termination("Input file/dir '" + inputArg
                    + "' does not exist or is not readable.");
        }

        if (NioUtils.checkFile(inputArg, IS_DIRECTORY)) {
            if (line.hasOption(OPTION_WORKINGDIR.getLongOpt())) {
                throw new Termination(
                        "Can't use --"
                                + OPTION_WORKINGDIR
                                + " (-w) argument if using existing working directory as input.");
            }

            workingDir = inputArg;
            getAndCheckCorpusFile(workingDir, "status");
            corpus = getAndCheckCorpusFile(workingDir, "training");
        } else {
            workingDir =
                    line.hasOption(OPTION_WORKINGDIR.getLongOpt()) ? Paths
                            .get(line.getOptionValue(OPTION_WORKINGDIR
                                    .getLongOpt())) : Paths.get(inputArg
                                            + ".out");
                            if (NioUtils.checkFile(workingDir, EXISTS, IS_NO_DIRECTORY)) {
                                System.err.println("Working directory '" + workingDir
                                        + "' already exists but is not a directory.");
                            }

                            corpus = inputArg;
        }

        if (line.hasOption(OPTION_MODELSIZE.getLongOpt())) {
            boolean exception = false;
            try {
                modelSize =
                        Integer.valueOf(line.getOptionValue(OPTION_MODELSIZE
                                .getLongOpt()));
            } catch (NumberFormatException e) {
                exception = true;
            }
            if (exception || modelSize <= 0) {
                throw new Termination("Unkown model size '"
                        + line.getOptionValue(OPTION_MODELSIZE.getLongOpt())
                        + "'. Need to be a positive integer.");
            }
        }

        if (line.hasOption(OPTION_MODEL.getLongOpt())) {
            for (String modelOpt : line.getOptionValues(OPTION_MODEL
                    .getLongOpt())) {
                Model model = Model.fromAbbreviation(modelOpt.toUpperCase());
                if (model == null) {
                    throw new Termination("Unkown models option '" + modelOpt
                            + "'.");
                }
                models.add(model);
            }
        }

        if (line.hasOption(OPTION_TESTING.getLongOpt())) {
            for (String testingOpt : line.getOptionValues(OPTION_TESTING
                    .getLongOpt())) {
                Path path = Paths.get(testingOpt.trim());
                if (!NioUtils.checkFile(path, EXISTS, IS_READABLE,
                        IS_REGULAR_FILE)) {
                    throw new Termination("Testing file '" + path
                            + "' does not exist or is not readable.");
                }
                testingFiles.add(path);
            }
        }
    }

    private Path getAndCheckCorpusFile(Path workingDir, String filename) {
        Path file = workingDir.resolve(filename);
        if (!NioUtils.checkFile(file, EXISTS, IS_READABLE)) {
            throw new Termination(filename + " file '" + file
                    + "' does not exist or is not readable.");
        }
        return file;
    }

    @Override
    protected void configureLogging() {
        super.configureLogging();
        LogUtils.addLocalFileAppender(workingDir.resolve("log"));
    }

    @Override
    protected void exec() throws IOException {
        Glmtk glmtk = new Glmtk(corpus, workingDir);

        boolean needPos = false;

        ProbMode probMode = ProbMode.MARG;

        if (models.isEmpty()) {
            models.add(Model.MODIFIED_KNESER_NEY);
        }

        Set<Pattern> neededPatterns = new HashSet<Pattern>();
        for (Model model : models) {
            Estimator estimator = model.getEstimator();

            neededPatterns.addAll(Patterns.getUsedPatterns(modelSize,
                    estimator, probMode));
            if (needPos) {
                neededPatterns.addAll(Patterns.getPosPatterns(neededPatterns));
            }
        }

        glmtk.count(needPos, neededPatterns);

        if (!testingFiles.isEmpty()) {
            for (Path testingFile : testingFiles) {
                for (Model model : models) {
                    Estimator estimator = model.getEstimator();

                    CountCache countCache =
                            glmtk.getOrCreateTestCountCache(testingFile,
                                    neededPatterns);
                    glmtk.test(testingFile, estimator, probMode, countCache);
                }
            }
        }

        StatisticalNumberHelper.print();
    }
}
