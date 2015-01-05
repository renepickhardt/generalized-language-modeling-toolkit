package de.glmtk.executables;

import static de.glmtk.common.Output.OUTPUT;
import static de.glmtk.util.LoggingHelper.LOGGING_HELPER;
import static de.glmtk.util.NioUtils.CheckFile.EXISTS;
import static de.glmtk.util.NioUtils.CheckFile.IS_DIRECTORY;
import static de.glmtk.util.NioUtils.CheckFile.IS_NO_DIRECTORY;
import static de.glmtk.util.NioUtils.CheckFile.IS_READABLE;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.cli.Option;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.ConsoleAppender.Target;

import de.glmtk.Constants;
import de.glmtk.Glmtk;
import de.glmtk.Termination;
import de.glmtk.common.CountCache;
import de.glmtk.common.Pattern;
import de.glmtk.common.Patterns;
import de.glmtk.common.ProbMode;
import de.glmtk.querying.QueryType;
import de.glmtk.querying.estimator.Estimator;
import de.glmtk.querying.estimator.Estimators;
import de.glmtk.util.NioUtils;
import de.glmtk.util.StatisticalNumberHelper;
import de.glmtk.util.StringUtils;

public class GlmtkExecutable extends Executable {
    private static final Logger LOGGER = LogManager.getFormatterLogger(Executable.class);

    // TODO: API to count all patterns.
    // TODO: API to only create querycache
    // TODO: API for interactive mode

    public static final Map<String, Estimator> OPTION_ESTIMATOR_ARGUMENTS;
    static {
        Map<String, Estimator> m = new LinkedHashMap<String, Estimator>();
        m.put("MLE", Estimators.MLE);
        m.put("MKN", Estimators.MOD_KNESER_NEY);
        m.put("MKNS", Estimators.MOD_KNESER_NEY_SKP);
        m.put("MKNA", Estimators.MOD_KNESER_NEY_ABS);
        m.put("GLM", Estimators.GLM);
        m.put("GLMD", Estimators.GLM_DEL);
        m.put("GLMDF", Estimators.GLM_DEL_FRONT);
        m.put("GLMSD", Estimators.GLM_SKP_AND_DEL);
        m.put("GLMA", Estimators.GLM_ABS);
        OPTION_ESTIMATOR_ARGUMENTS = m;
    }

    private static final Option OPTION_HELP;
    private static final Option OPTION_VERSION;
    private static final Option OPTION_WORKINGDIR;
    private static final Option OPTION_TRAINING_ORDER;
    private static final Option OPTION_ESTIMATOR;
    private static final Option OPTION_INTERACTIVE_SEQUENCE;
    private static final Option OPTION_INTERACTIVE_MARKOV;
    private static final Option OPTION_INTERACTIVE_COND;
    private static final Option OPTION_QUERY_SEQUENCE;
    private static final Option OPTION_QUERY_MARKOV;
    private static final Option OPTION_QUERY_COND;
    private static final Option OPTION_LOG_CONSOLE;
    private static final Option OPTION_LOG_DEBUG;

    private static final List<Option> OPTIONS;
    private static final List<Option> OPTIONS_INTERACTIVE;
    private static final List<Option> OPTIONS_QUERYING;

    static {
        OPTION_HELP = new Option(OPTION_HELP_SHORT, OPTION_HELP_LONG, false,
                "Print this message.");

        OPTION_VERSION = new Option(OPTION_VERSION_SHORT, OPTION_VERSION_LONG,
                false, "Print the version information and exit.");

        OPTION_WORKINGDIR = new Option("w", "workingdir", true,
                "Working directory.");
        OPTION_WORKINGDIR.setArgName("WORKINGDIR");

        OPTION_TRAINING_ORDER = new Option("n", "training-order", true,
                "Order to learn for training.");
        OPTION_TRAINING_ORDER.setArgName("ORDER");

        try (Formatter estimatorDesc = new Formatter()) {
            estimatorDesc.format("Can be specified multiple times.%n");
            for (Entry<String, Estimator> arg : OPTION_ESTIMATOR_ARGUMENTS.entrySet())
                estimatorDesc.format("%-5s - %s%n", arg.getKey(),
                        arg.getValue().getName());
            OPTION_ESTIMATOR = new Option("e", "estimator", true,
                    estimatorDesc.toString());
            OPTION_ESTIMATOR.setArgName("ESTIMATOR...");
            OPTION_ESTIMATOR.setArgs(Option.UNLIMITED_VALUES);
        }

        OPTION_INTERACTIVE_SEQUENCE = new Option("is", "interactive-seuence",
                false, "Takes sequence queries from standart input.");

        OPTION_INTERACTIVE_MARKOV = new Option("im", "interactive-markov",
                true, "Takes markov queries from standart input.");
        OPTION_INTERACTIVE_MARKOV.setArgName("MARKOV");
        OPTION_INTERACTIVE_MARKOV.setArgs(1);

        OPTION_INTERACTIVE_COND = new Option("ic", "interactive-cond", true,
                "Takes conditional queries from standart input.");
        OPTION_INTERACTIVE_COND.setArgName("COND");
        OPTION_INTERACTIVE_COND.setArgs(1);

        OPTION_QUERY_SEQUENCE = new Option("qs", "query-sequence", true,
                "Files to take querying sequences from.");
        OPTION_QUERY_SEQUENCE.setArgName("FILE...");
        OPTION_QUERY_SEQUENCE.setArgs(Option.UNLIMITED_VALUES);

        OPTION_QUERY_MARKOV = new Option("qm", "query-markov", true,
                "Files to take quering markov sequences from.");
        OPTION_QUERY_MARKOV.setArgName("MARKOV> <FILE...");
        OPTION_QUERY_MARKOV.setArgs(Option.UNLIMITED_VALUES);

        OPTION_QUERY_COND = new Option("qc", "query-cond", true,
                "Files to take querying conditional seuqences from.");
        OPTION_QUERY_COND.setArgName("ORDER> <FILE...");
        OPTION_QUERY_COND.setArgs(Option.UNLIMITED_VALUES);

        OPTION_LOG_CONSOLE = new Option(null, "log", false,
                "If set will also log to console");

        OPTION_LOG_DEBUG = new Option(null, "debug", false,
                "If set, log level will be increased to 'Debug'.");

        OPTIONS = Arrays.asList(OPTION_HELP, OPTION_VERSION, OPTION_WORKINGDIR,
                OPTION_TRAINING_ORDER, OPTION_ESTIMATOR,
                OPTION_INTERACTIVE_SEQUENCE, OPTION_INTERACTIVE_MARKOV,
                OPTION_INTERACTIVE_COND, OPTION_QUERY_SEQUENCE,
                OPTION_QUERY_MARKOV, OPTION_QUERY_COND, OPTION_LOG_CONSOLE,
                OPTION_LOG_DEBUG);
        OPTIONS_INTERACTIVE = Arrays.asList(OPTION_INTERACTIVE_SEQUENCE,
                OPTION_INTERACTIVE_MARKOV, OPTION_INTERACTIVE_COND);
        OPTIONS_QUERYING = Arrays.asList(OPTION_QUERY_SEQUENCE,
                OPTION_QUERY_MARKOV, OPTION_QUERY_COND);
    }

    public static void main(String[] args) throws Exception {
        new GlmtkExecutable().run(args);
    }

    private Path corpus = null;
    private Path workingDir = null;
    private Integer trainingOrder = null;
    private Set<Estimator> estimators = new LinkedHashSet<Estimator>();
    private String interactiveQueryType = null;
    private int interactiveCondOrder = 0;
    private Set<Path> querySequenceFiles = new LinkedHashSet<Path>();
    private Map<Integer, Set<Path>> queryMarkovFiles = new HashMap<Integer, Set<Path>>();
    private Map<Integer, Set<Path>> queryCondFiles = new HashMap<Integer, Set<Path>>();
    private boolean logConsole = false;
    private boolean logDebug = false;

    @Override
    protected List<Option> getOptions() {
        return OPTIONS;
    }

    @Override
    protected String getUsage() {
        return "glmtk <INPUT> [OPTION]...";
    }

    @Override
    protected void parseArguments(String[] args) throws Exception {
        super.parseArguments(args);

        if (line.getArgList() == null || line.getArgList().size() != 1) {
            String error;
            if (line.getArgList().size() == 0)
                error = "Missing input.\n";
            else
                error = String.format("Incorrect input: %s%n",
                        StringUtils.join(line.getArgList(), " "));
            throw new Termination(error
                    + "Try 'glmtk --help' for more information.");
        }

        Path inputArg = Paths.get(line.getArgs()[0]);
        if (!NioUtils.checkFile(inputArg, EXISTS, IS_READABLE))
            throw new Termination(String.format(
                    "Input file/dir '%s' does not exist or is not readable.",
                    inputArg));

        for (Option option : line.getOptions())
            if (option.equals(OPTION_WORKINGDIR)) {
                checkOptionMultipleTimes(workingDir, option);
                workingDir = Paths.get(option.getValue());

            } else if (option.equals(OPTION_TRAINING_ORDER)) {
                checkOptionMultipleTimes(trainingOrder, option);
                trainingOrder = positiveIntOrFail(option.getValue(),
                        "Illegal --" + option.getLongOpt() + " argument");

            } else if (option.equals(OPTION_ESTIMATOR))
                for (String opt : option.getValues()) {
                    Estimator estimator = OPTION_ESTIMATOR_ARGUMENTS.get(opt.toUpperCase());
                    if (estimator == null)
                        throw new Termination(String.format(
                                "Unkown estimators option '%s'.", opt));
                    estimators.add(estimator);
                }

            else if (option.equals(OPTION_INTERACTIVE_SEQUENCE)) {
                optionsMutuallyExclusiveOrFail(interactiveQueryType,
                        OPTIONS_INTERACTIVE);
                interactiveQueryType = QueryType.sequence();

            } else if (option.equals(OPTION_INTERACTIVE_MARKOV)) {
                optionsMutuallyExclusiveOrFail(interactiveQueryType,
                        OPTIONS_INTERACTIVE);
                interactiveQueryType = QueryType.markov(positiveIntOrFail(
                        option.getValue(), "Illegal argument for --%s (-%s)",
                        option.getLongOpt(), option.getOpt()));

            } else if (option.equals(OPTION_INTERACTIVE_COND)) {
                optionsMutuallyExclusiveOrFail(interactiveQueryType,
                        OPTIONS_INTERACTIVE);
                interactiveQueryType = QueryType.cond();
                interactiveCondOrder = positiveIntOrFail(option.getValue(),
                        "Illegal argument for --%s (-%s)", option.getLongOpt(),
                        option.getOpt());

            } else if (option.equals(OPTION_QUERY_SEQUENCE))
                for (String opt : option.getValues())
                    querySequenceFiles.add(Paths.get(opt.trim()));

            else if (option.equals(OPTION_QUERY_MARKOV))
                extractOrderAndFiles(option, queryMarkovFiles);

            else if (option.equals(OPTION_QUERY_COND))
                extractOrderAndFiles(option, queryCondFiles);

            else if (option.equals(OPTION_LOG_CONSOLE))
                logConsole = true;

            else if (option.equals(OPTION_LOG_DEBUG))
                logDebug = true;

            else
                throw new IllegalStateException(String.format(
                        "Unexpected option: '%s'.", option));

        if (NioUtils.checkFile(inputArg, IS_DIRECTORY)) {
            if (workingDir != null)
                throw new Termination(
                        String.format(
                                "Can't use --%s (-%s) argument if using existing working directory as input.",
                                OPTION_WORKINGDIR.getLongOpt(),
                                OPTION_WORKINGDIR.getOpt()));

            workingDir = inputArg;
            corpus = getAndCheckFile(Constants.TRAINING_FILE_NAME);
            getAndCheckFile(Constants.STATUS_FILE_NAME);
        } else {
            if (workingDir == null)
                workingDir = Paths.get(inputArg + Constants.WORKING_DIR_SUFFIX);
            if (NioUtils.checkFile(workingDir, EXISTS, IS_NO_DIRECTORY))
                throw new Termination(
                        String.format(
                                "Working directory '%s' already exists but is not a directory.",
                                workingDir));

            corpus = inputArg;
        }

        if (trainingOrder == null)
            trainingOrder = 5;

        if (estimators.isEmpty())
            estimators.add(OPTION_ESTIMATOR_ARGUMENTS.values().iterator().next());

        if (interactiveQueryType != null
                && !(querySequenceFiles.isEmpty() && queryMarkovFiles.isEmpty() && queryCondFiles.isEmpty()))
            throw new Termination(
                    String.format(
                            "Illegal options, can not specify interactive mode(%s) and qyering mode(%s) at the same time.",
                            makeOptionsString(OPTIONS_INTERACTIVE),
                            makeOptionsString(OPTIONS_QUERYING)));

        if (interactiveQueryType != null && estimators.size() > 1)
            throw new Termination(
                    String.format(
                            "Can specify at most one estimator in interactive mode (%s).",
                            makeOptionsString(OPTIONS_INTERACTIVE)));

        // Need to create workingDirectory here in order to create Logger for
        // "<workingdir>/log" as soon as possible.
        Files.createDirectories(workingDir);

        configureLogging();

        if (logDebug)
            LOGGING_HELPER.setLogLevel(Level.DEBUG);

        verifyQueryFiles();
    }

    private String makeOptionString(Option option) {
        return String.format("--%s (-%s)", option.getLongOpt(), option.getOpt());
    }

    private String makeOptionsString(Collection<Option> options) {
        List<String> optionsStr = new ArrayList<String>(options.size());
        for (Option option : options)
            optionsStr.add(makeOptionString(option));
        return StringUtils.join(optionsStr, ", ");
    }

    private void checkOptionMultipleTimes(Object value,
                                          Option option) {
        if (value != null)
            throw new Termination(String.format(
                    "Option %s must not be specified more than once.",
                    makeOptionString(option)));
    }

    private <T> void optionsMutuallyExclusiveOrFail(T value,
                                                    Collection<Option> options) {
        if (value != null)
            throw new Termination(String.format(
                    "The options %s are mutually exclusive.",
                    makeOptionsString(options)));
    }

    private int positiveIntOrFail(String value,
                                  String message,
                                  Object... params) {
        Integer v = null;
        try {
            v = Integer.valueOf(value);
        } catch (NumberFormatException e) {
        }
        if (v == null || v <= 0)
            try (Formatter f = new Formatter()) {
                f.format(message, params);
                f.format(" '%s'.%n", value);
                f.format("Needs to be a positive integer");
                throw new Termination(f.toString());
            }
        return v;
    }

    private void extractOrderAndFiles(Option option,
                                      Map<Integer, Set<Path>> orderToFiles) {
        String[] opts = option.getValues();
        int order = positiveIntOrFail(opts[0],
                "Illegal first argument for --%s (-%s)", option.getLongOpt(),
                option.getOpt());
        Set<Path> files = orderToFiles.get(order);
        if (files == null) {
            files = new LinkedHashSet<Path>();
            orderToFiles.put(order, files);
        }
        for (int i = 1; i != opts.length; ++i)
            files.add(Paths.get(opts[i].trim()));
    }

    private Path getAndCheckFile(String filename) {
        Path file = workingDir.resolve(filename);
        if (!NioUtils.checkFile(file, EXISTS, IS_READABLE))
            throw new Termination(String.format(
                    "%s file '%s' does not exist or is not readable.",
                    filename, file));
        return file;
    }

    private void configureLogging() {
        LOGGING_HELPER.addFileAppender(
                workingDir.resolve(Constants.LOCAL_LOG_FILE_NAME), "FileLocal",
                true);
        if (logConsole) {
            LOGGING_HELPER.addConsoleAppender(Target.SYSTEM_ERR);
            // Stop clash of Log Messages with CondoleOutputter's Ansi Control Codes.
            OUTPUT.disableAnsi();
        }
    }

    private void verifyQueryFiles() throws Exception {
        for (Integer markovOrder : queryMarkovFiles.keySet())
            if (markovOrder > trainingOrder)
                throw new Exception(
                        String.format(
                                "Illegal markov query order '%d' (-%s, --%s): Higher than training order '%d' (-%s, --%s).",
                                markovOrder, OPTION_QUERY_MARKOV.getOpt(),
                                OPTION_QUERY_MARKOV.getLongOpt(),
                                trainingOrder, OPTION_TRAINING_ORDER.getOpt(),
                                OPTION_TRAINING_ORDER.getLongOpt()));

        for (Integer condOrder : queryCondFiles.keySet())
            if (condOrder > trainingOrder)
                throw new Exception(
                        String.format(
                                "Illegal conditional query order '%d' (-%s, --%s): Higher than training order '%d' (-%s, --%s).",
                                condOrder, OPTION_QUERY_COND.getOpt(),
                                OPTION_QUERY_COND.getLongOpt(), trainingOrder,
                                OPTION_TRAINING_ORDER.getOpt(),
                                OPTION_TRAINING_ORDER.getLongOpt()));

        for (Entry<Integer, Set<Path>> condQuery : queryCondFiles.entrySet()) {
            int condOrder = condQuery.getKey();
            Set<Path> queryFiles = condQuery.getValue();

            for (Path queryFile : queryFiles)
                verifyFileHasCondOrder(queryFile, condOrder);
        }
    }

    private void verifyFileHasCondOrder(Path queryFile,
                                        int condOrder) throws Exception {
        try (BufferedReader reader = Files.newBufferedReader(queryFile,
                Constants.CHARSET)) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                ++lineNo;

                List<String> words = StringUtils.splitAtChar(line, ' ');

                if (words.size() != condOrder)
                    try (Formatter f = new Formatter()) {
                        f.format("Illegal line '%d' in file '%s'.%n", lineNo,
                                queryFile);
                        f.format(
                                "Line does not have specified conditional query order '%d' (-%s, --%s).%n",
                                condOrder, OPTION_QUERY_COND.getOpt(),
                                OPTION_QUERY_COND.getLongOpt());
                        f.format("Found order: '%d'.%n", words.size());
                        f.format("Line was: '%s'.", line);
                        throw new Exception(f.toString());
                    }
            }
        }
    }

    @Override
    protected void exec() throws Exception {
        logFields();

        Glmtk glmtk = new Glmtk(corpus, workingDir);

        boolean needPos = false;

        ProbMode probMode = ProbMode.MARG;

        Set<Pattern> neededPatterns = new HashSet<Pattern>();
        for (Estimator estimator : estimators) {
            neededPatterns.addAll(Patterns.getUsedPatterns(trainingOrder,
                    estimator, probMode));
            if (needPos)
                neededPatterns.addAll(Patterns.getPosPatterns(neededPatterns));
        }

        glmtk.count(neededPatterns);

        if (interactiveQueryType != null) {
            Estimator estimator = estimators.iterator().next();
            CountCache countCache = glmtk.createCountCache(neededPatterns);
            glmtk.runQueriesOnInputStream(interactiveQueryType, System.in,
                    System.out, estimator, probMode, countCache);
        }

        for (Path queryFile : querySequenceFiles) {
            CountCache countCache = glmtk.provideQueryCache(queryFile,
                    neededPatterns);

            for (Estimator estimator : estimators)
                glmtk.runQueriesOnFile(QueryType.sequence(), queryFile,
                        estimator, probMode, countCache);
        }

        for (Map.Entry<Integer, Set<Path>> entry : queryMarkovFiles.entrySet()) {
            int markovOrder = entry.getKey();
            Set<Path> queryFiles = entry.getValue();

            for (Path queryFile : queryFiles) {
                CountCache countCache = glmtk.provideQueryCache(queryFile,
                        neededPatterns);

                for (Estimator estimator : estimators)
                    glmtk.runQueriesOnFile(QueryType.markov(markovOrder),
                            queryFile, estimator, probMode, countCache);
            }
        }

        for (Set<Path> queryFiles : queryCondFiles.values())
            for (Path queryFile : queryFiles) {
                CountCache countCache = glmtk.provideQueryCache(queryFile,
                        neededPatterns);

                for (Estimator estimator : estimators)
                    glmtk.runQueriesOnFile(QueryType.cond(), queryFile,
                            estimator, probMode, countCache);
            }

        StatisticalNumberHelper.print();
    }

    private void logFields() {
        LOGGER.debug("GlmtkExecutables Fields %s", StringUtils.repeat("-",
                "GlmtkExecutables Fields ".length()));
        LOGGER.debug("Corpus:               %s", corpus);
        LOGGER.debug("WorkingDir:           %s", workingDir);
        LOGGER.debug("TrainingOrder:        %s", trainingOrder);
        LOGGER.debug("InteractiveQueryType: %s", interactiveQueryType);
        LOGGER.debug("InteractiveCondOrder: %s", interactiveCondOrder);
        LOGGER.debug("QuerySequenceFiles:   %s", querySequenceFiles);
        LOGGER.debug("QueryMarkovFiles:     %s", queryMarkovFiles);
        LOGGER.debug("QueryCondFiles:       %s", queryCondFiles);
        LOGGER.debug("LogConsole:           %s", logConsole);
        LOGGER.debug("LogDebug:             %s", logDebug);
    }
}
