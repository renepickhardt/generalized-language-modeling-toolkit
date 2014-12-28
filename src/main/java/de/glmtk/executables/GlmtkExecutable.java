package de.glmtk.executables;

import static de.glmtk.ConsoleOutputter.CONSOLE_OUTPUTTER;
import static de.glmtk.utils.LogUtils.LOG_UTILS;
import static de.glmtk.utils.NioUtils.CheckFile.EXISTS;
import static de.glmtk.utils.NioUtils.CheckFile.IS_DIRECTORY;
import static de.glmtk.utils.NioUtils.CheckFile.IS_NO_DIRECTORY;
import static de.glmtk.utils.NioUtils.CheckFile.IS_READABLE;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.Option;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.ConsoleAppender.Target;

import de.glmtk.Constants;
import de.glmtk.Glmtk;
import de.glmtk.Model;
import de.glmtk.Termination;
import de.glmtk.counting.CountCache;
import de.glmtk.querying.ProbMode;
import de.glmtk.querying.estimator.Estimator;
import de.glmtk.utils.NioUtils;
import de.glmtk.utils.Pattern;
import de.glmtk.utils.Patterns;
import de.glmtk.utils.StatisticalNumberHelper;
import de.glmtk.utils.StringUtils;

public class GlmtkExecutable extends Executable {

    private static Logger LOGGER = LogManager.getLogger(Executable.class);

    // TODO: API to count all patterns.

    private static final Option OPTION_HELP;

    private static final Option OPTION_VERSION;

    private static final Option OPTION_WORKINGDIR;

    private static final Option OPTION_TRAINING_ORDER;

    private static final Option OPTION_MODEL;

    private static final Option OPTION_TEST_SENTENCE;

    private static final Option OPTION_TEST_MARKOV;

    private static final Option OPTION_TEST_COND;

    private static final Option OPTION_LOG;

    private static final List<Option> OPTIONS;

    static {
        OPTION_HELP =
                new Option(OPTION_HELP_SHORT, OPTION_HELP_LONG, false,
                        "Print this message.");

        OPTION_VERSION =
                new Option(OPTION_VERSION_SHORT, OPTION_VERSION_LONG, false,
                        "Print the version information and exit.");

        OPTION_WORKINGDIR =
                new Option("w", "workingdir", true, "Working directory.");
        OPTION_WORKINGDIR.setArgName("WORKINGDIR");

        StringBuilder modelOptDesc = new StringBuilder();
        for (Model model : Model.values()) {
            String abbreviation = model.getAbbreviation();
            modelOptDesc.append(abbreviation);
            modelOptDesc.append(StringUtils.repeat(" ",
                    5 - abbreviation.length()));
            modelOptDesc.append("- ");
            modelOptDesc.append(model.getName());
            modelOptDesc.append(".\n");
        }
        OPTION_TRAINING_ORDER =
                new Option("n", "training-order", true,
                        "Order to learn for training.");
        OPTION_TRAINING_ORDER.setArgName("ORDER");

        OPTION_MODEL =
                new Option("m", "model", true,
                        "Can be specified multiple times.\n"
                                + modelOptDesc.toString());
        OPTION_MODEL.setArgName("MODEL...");
        OPTION_MODEL.setArgs(Option.UNLIMITED_VALUES);

        OPTION_TEST_SENTENCE =
                new Option("ts", "test-sentence", true,
                        "Files to take testing sentences from.");
        OPTION_TEST_SENTENCE.setArgName("FILE...");
        OPTION_TEST_SENTENCE.setArgs(Option.UNLIMITED_VALUES);

        OPTION_TEST_MARKOV =
                new Option("tm", "test-markov", true,
                        "Files to take testing markov sentences from.");
        OPTION_TEST_MARKOV.setArgName("MARKOV> <FILE...");
        OPTION_TEST_MARKOV.setArgs(Option.UNLIMITED_VALUES);

        OPTION_TEST_COND =
                new Option("tc", "test-cond", true,
                        "Files to take testing conditional seuqences from.");
        OPTION_TEST_COND.setArgName("ORDER> <FILE...");
        OPTION_TEST_COND.setArgs(Option.UNLIMITED_VALUES);

        OPTION_LOG =
                new Option(null, "log", false,
                        "If set will also log to console");

        OPTIONS =
                Arrays.asList(OPTION_HELP, OPTION_VERSION, OPTION_WORKINGDIR,
                        OPTION_TRAINING_ORDER, OPTION_MODEL,
                        OPTION_TEST_SENTENCE, OPTION_TEST_MARKOV,
                        OPTION_TEST_COND, OPTION_LOG);
    }

    private Path corpus = null;

    private Path workingDir = null;

    private Integer trainingOrder = null;

    private Set<Model> models = new LinkedHashSet<Model>();

    private Set<Path> testSentenceFiles = new LinkedHashSet<Path>();

    private Map<Integer, Set<Path>> testMarkovFiles =
            new HashMap<Integer, Set<Path>>();

    private Map<Integer, Set<Path>> testCondFiles =
            new HashMap<Integer, Set<Path>>();

    private boolean logToConsole = false;

    @Override
    protected List<Option> getOptions() {
        return OPTIONS;
    }

    @Override
    protected String getUsage() {
        return "glmtk <INPUT> [OPTION]...";
    }

    @Override
    protected void parseArguments(String[] args) {
        super.parseArguments(args);

        if (line.getArgList() == null || line.getArgList().size() != 1) {
            String error;
            if (line.getArgList().size() == 0) {
                error = "Missing input.\n";
            } else {
                error =
                        "Incorrect input: "
                                + StringUtils.join(line.getArgList(), " ")
                                + "\n";
            }
            throw new Termination(error
                    + "Try 'glmtk --help' for more information.");
        }

        Path inputArg = Paths.get(line.getArgs()[0]);
        if (!NioUtils.checkFile(inputArg, EXISTS, IS_READABLE)) {
            throw new Termination("Input file/dir '" + inputArg
                    + "' does not exist or is not readable.");
        }

        for (Option option : line.getOptions()) {
            if (option.equals(OPTION_WORKINGDIR)) {
                checkOptionMultipleTimes(workingDir, option);
                workingDir = Paths.get(option.getValue());

            } else if (option.equals(OPTION_TRAINING_ORDER)) {
                checkOptionMultipleTimes(trainingOrder, option);
                trainingOrder =
                        convertToPositiveInteger(option.getValue(),
                                "Illegal --" + option.getLongOpt()
                                        + " argument");

            } else if (option.equals(OPTION_MODEL)) {
                for (String opt : option.getValues()) {
                    Model model = Model.fromAbbreviation(opt.toUpperCase());
                    if (model == null) {
                        throw new Termination("Unkown models option '" + opt
                                + "'.");
                    }
                    models.add(model);
                }

            } else if (option.equals(OPTION_TEST_SENTENCE)) {
                for (String opt : option.getValues()) {
                    testSentenceFiles.add(Paths.get(opt.trim()));
                }

            } else if (option.equals(OPTION_TEST_MARKOV)) {
                extractOrderAndFiles(option, testMarkovFiles);

            } else if (option.equals(OPTION_TEST_COND)) {
                extractOrderAndFiles(option, testCondFiles);

            } else if (option.equals(OPTION_LOG)) {
                logToConsole = true;

            } else {
                throw new IllegalStateException("Unexpected option: " + option
                        + ".");
            }
        }

        if (NioUtils.checkFile(inputArg, IS_DIRECTORY)) {
            if (workingDir != null) {
                throw new Termination(
                        "Can't use --"
                                + OPTION_WORKINGDIR.getLongOpt()
                                + " (-w) argument if using existing working directory as input.");
            }

            workingDir = inputArg;
            corpus = getAndCheckFile(Constants.TRAINING_FILE_NAME);
            getAndCheckFile(Constants.STATUS_FILE_NAME);
        } else {
            if (workingDir == null) {
                workingDir = Paths.get(inputArg + ".out");
            }
            if (NioUtils.checkFile(workingDir, EXISTS, IS_NO_DIRECTORY)) {
                throw new Termination("Working directory '" + workingDir
                        + "' already exists but is not a directory.");
            }

            corpus = inputArg;
        }

        if (trainingOrder == null) {
            trainingOrder = 5;
        }
    }

    private void checkOptionMultipleTimes(Object value, Option option) {
        if (value != null) {
            throw new Termination("Option --" + option.getLongOpt()
                    + " can only be specified once.");
        }
    }

    private int convertToPositiveInteger(String val, String msg) {
        Integer v = null;
        try {
            v = Integer.valueOf(val);
        } catch (NumberFormatException e) {
        }
        if (v == null || v <= 0) {
            throw new Termination(msg + " '" + val
                    + "'. Needs to be a positive integer.");
        }
        return v;
    }

    private void extractOrderAndFiles(
            Option option,
            Map<Integer, Set<Path>> orderToFiles) {
        String[] opts = option.getValues();
        int order =
                convertToPositiveInteger(opts[0],
                        "Illegal first argument for --" + option.getLongOpt());
        Set<Path> files = orderToFiles.get(order);
        if (files == null) {
            files = new LinkedHashSet<Path>();
            orderToFiles.put(order, files);
        }
        for (int i = 1; i != opts.length; ++i) {
            files.add(Paths.get(opts[i].trim()));
        }
    }

    private Path getAndCheckFile(String filename) {
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
        LOG_UTILS.addFileAppender(
                workingDir.resolve(Constants.LOCAL_LOG_FILE_NAME), "FileLocal",
                true);
        if (logToConsole) {
            LOG_UTILS.addConsoleAppender(Target.SYSTEM_ERR);
            // Stop clash of Log Messages with CondoleOutputter's Ansi Control Codes.
            CONSOLE_OUTPUTTER.disableAnsi();
        }
    }

    @Override
    protected void exec() throws IOException {
        logOptions();

        Glmtk glmtk = new Glmtk(corpus, workingDir);

        boolean needPos = false;

        ProbMode probMode = ProbMode.MARG;

        if (models.isEmpty()) {
            models.add(Model.MODIFIED_KNESER_NEY);
        }

        Set<Pattern> neededPatterns = new HashSet<Pattern>();
        for (Model model : models) {
            Estimator estimator = model.getEstimator();

            neededPatterns.addAll(Patterns.getUsedPatterns(trainingOrder,
                    estimator, probMode));
            if (needPos) {
                neededPatterns.addAll(Patterns.getPosPatterns(neededPatterns));
            }
        }

        glmtk.count(needPos, neededPatterns);

        for (Path testFile : testSentenceFiles) {
            CountCache countCache =
                    glmtk.getOrCreateTestCountCache(testFile, neededPatterns);

            for (Model model : models) {
                Estimator estimator = model.getEstimator();
                glmtk.testSentenceFile(testFile, estimator, probMode,
                        countCache);
            }
        }

        for (Map.Entry<Integer, Set<Path>> entry : testMarkovFiles.entrySet()) {
            int order = entry.getKey();
            Set<Path> testFiles = entry.getValue();

            for (Path testFile : testFiles) {
                CountCache countCache =
                        glmtk.getOrCreateTestCountCache(testFile,
                                neededPatterns);

                for (Model model : models) {
                    Estimator estimator = model.getEstimator();
                    glmtk.testMarkovFile(testFile, estimator, probMode,
                            countCache, order);
                }
            }
        }

        for (Map.Entry<Integer, Set<Path>> entry : testCondFiles.entrySet()) {
            int order = entry.getKey();
            Set<Path> testFiles = entry.getValue();

            for (Path testFile : testFiles) {
                CountCache countCache =
                        glmtk.getOrCreateTestCountCache(testFile,
                                neededPatterns);

                for (Model model : models) {
                    Estimator estimator = model.getEstimator();
                    glmtk.testCondFile(testFile, estimator, probMode,
                            countCache, order);
                }
            }
        }

        StatisticalNumberHelper.print();
    }

    private void logOptions() {
        LOGGER.debug("Corpus:            {}", corpus);
        LOGGER.debug("WorkingDir:        {}", workingDir);
        LOGGER.debug("TrainingOrder:     {}", trainingOrder);
        LOGGER.debug("TestSentenceFiles: {}", testSentenceFiles);
        LOGGER.debug("TestMarkovFiles:   {}", testMarkovFiles);
        LOGGER.debug("TestCondFiles:     {}", testCondFiles);
    }

    public static void main(String[] args) throws Exception {
        new GlmtkExecutable().run(args);
    }

}
