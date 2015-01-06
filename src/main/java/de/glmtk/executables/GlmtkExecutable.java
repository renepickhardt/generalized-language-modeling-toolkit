package de.glmtk.executables;

import static de.glmtk.common.Output.OUTPUT;
import static de.glmtk.util.LoggingHelper.LOGGING_HELPER;
import static de.glmtk.util.NioUtils.CheckFile.EXISTS;
import static de.glmtk.util.NioUtils.CheckFile.IS_DIRECTORY;
import static de.glmtk.util.NioUtils.CheckFile.IS_NO_DIRECTORY;
import static de.glmtk.util.NioUtils.CheckFile.IS_READABLE;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
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
import de.glmtk.common.CountCache;
import de.glmtk.common.Pattern;
import de.glmtk.common.Patterns;
import de.glmtk.common.ProbMode;
import de.glmtk.exceptions.CliArgumentException;
import de.glmtk.querying.QueryMode;
import de.glmtk.querying.estimator.Estimator;
import de.glmtk.querying.estimator.Estimators;
import de.glmtk.util.NioUtils;
import de.glmtk.util.StatisticalNumberHelper;
import de.glmtk.util.StringUtils;

public class GlmtkExecutable extends Executable {
    private static final Logger LOGGER = LogManager.getFormatterLogger(Executable.class);

    private static final Option OPTION_HELP;
    private static final Option OPTION_VERSION;
    private static final Option OPTION_WORKINGDIR;
    private static final Option OPTION_TRAINING_ORDER;
    private static final Option OPTION_ESTIMATOR;
    private static final Option OPTION_IO;
    private static final Option OPTION_QUERY;
    private static final Option OPTION_LOG_CONSOLE;
    private static final Option OPTION_LOG_DEBUG;

    private static final List<Option> OPTIONS;

    static {
        OPTION_HELP = new Option(OPTION_HELP_SHORT, OPTION_HELP_LONG, false,
                "Print this message.");

        OPTION_VERSION = new Option(OPTION_VERSION_SHORT, OPTION_VERSION_LONG,
                false, "Print the version information and exit.");

        OPTION_WORKINGDIR = new Option("w", "workingdir", true,
                "Working directory.");
        OPTION_WORKINGDIR.setArgName("DIRECTORY");

        OPTION_TRAINING_ORDER = new Option("n", "training-order", true,
                "Order to learn for training.");
        OPTION_TRAINING_ORDER.setArgName("ORDER");

        OPTION_ESTIMATOR = new Option("e", "estimator", true,
                "Can be specified multiple times.");
        OPTION_ESTIMATOR.setArgName("ESTIMATOR...");
        OPTION_ESTIMATOR.setArgs(Option.UNLIMITED_VALUES);

        OPTION_IO = new Option("i", "io", true,
                "Takes queries from standart input with given mode.");
        OPTION_IO.setArgName("MODE");
        OPTION_IO.setArgs(1);

        OPTION_QUERY = new Option("q", "query", true,
                "Queries the given files with given mode.");
        OPTION_QUERY.setArgName("MODE> <FILE...");
        OPTION_QUERY.setArgs(Option.UNLIMITED_VALUES);

        OPTION_LOG_CONSOLE = new Option(null, "log", false,
                "If set will also log to console");

        OPTION_LOG_DEBUG = new Option(null, "debug", false,
                "If set, log level will be increased to 'Debug'.");

        OPTIONS = Arrays.asList(OPTION_HELP, OPTION_VERSION, OPTION_WORKINGDIR,
                OPTION_TRAINING_ORDER, OPTION_ESTIMATOR, OPTION_IO,
                OPTION_QUERY, OPTION_LOG_CONSOLE, OPTION_LOG_DEBUG);
    }

    public static final Map<String, Estimator> OPTION_ESTIMATOR_ARGUMENTS;
    static {
        Map<String, Estimator> m = new LinkedHashMap<>();
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

    public static void main(String[] args) {
        new GlmtkExecutable().run(args);
    }

    private Path corpus = null;
    private Path workingDir = null;
    private Integer trainingOrder = null;
    private Set<Estimator> estimators = new LinkedHashSet<>();
    private QueryMode ioQueryMode = null;
    private List<Entry<QueryMode, Set<Path>>> queries = new LinkedList<>();
    private boolean logConsole = false;
    private boolean logDebug = false;

    @Override
    protected List<Option> getOptions() {
        return OPTIONS;
    }

    @Override
    protected String getHelpHeader() {
        try (Formatter f = new Formatter()) {
            f.format("glmtk <INPUT> [<OPTION...>]%n");
            f.format("Invokes the Generalized Language Model Toolkit.%n");

            f.format("\nMandatory arguments to long options are mandatory for short options too.%n");

            return f.toString();
        }
    }

    @Override
    protected String getHelpFooter() {
        try (Formatter f = new Formatter()) {
            f.format("%nWhere <ORDER> may be any postive integer.%n");

            f.format("%nWhere <ESTIMATOR> may be any of:%n");
            for (Entry<String, Estimator> arg : OPTION_ESTIMATOR_ARGUMENTS.entrySet())
                f.format("  * %-5s  %s%n", arg.getKey(),
                        arg.getValue().getName());

            f.format("%nWhere <MODE> may be any of:%n");
            f.format("  * <ORDER>        P^n%n");
            f.format("  * Sequence       P^*%n");
            f.format("  * Markov<ORDER>  P^* with markov%n");
            f.format("  * Cond<ORDER>    Conditional P%n");

            f.format("%nFor more information, see:%n");
            f.format("https://github.com/renepickhardt/generalized-language-modeling-toolkit/%n");

            return f.toString();
        }
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
            throw new CliArgumentException(error
                    + "Try 'glmtk --help' for more information.");
        }

        Path inputArg = Paths.get(line.getArgs()[0]);
        if (!NioUtils.checkFile(inputArg, EXISTS, IS_READABLE))
            throw new CliArgumentException(String.format(
                    "Input file/dir '%s' does not exist or is not readable.",
                    inputArg));

        for (Option option : line.getOptions())
            if (option.equals(OPTION_WORKINGDIR)) {
                optionFirstTimeOrFail(workingDir, option);
                workingDir = Paths.get(option.getValue());

            } else if (option.equals(OPTION_TRAINING_ORDER)) {
                optionFirstTimeOrFail(trainingOrder, option);
                trainingOrder = positiveIntOrFail(option.getValue(),
                        "Illegal %s argument", makeOptionString(option));

            } else if (option.equals(OPTION_ESTIMATOR))
                for (String opt : option.getValues()) {
                    Estimator estimator = OPTION_ESTIMATOR_ARGUMENTS.get(opt.toUpperCase());
                    if (estimator == null)
                        throw new CliArgumentException(
                                String.format(
                                        "Illegal %s argument. Unkown estimators option '%s'.",
                                        makeOptionString(option), opt));
                    estimators.add(estimator);
                }

            else if (option.equals(OPTION_IO)) {
                optionFirstTimeOrFail(ioQueryMode, option);
                if (!queries.isEmpty())
                    throw new CliArgumentException(String.format(
                            "The options %s and %s are mutually exclusive.",
                            makeOptionString(OPTION_IO),
                            makeOptionString(OPTION_QUERY)));

                try {
                    ioQueryMode = QueryMode.forString(option.getValue());
                } catch (RuntimeException e) {
                    throw new CliArgumentException(String.format(
                            "Illegal %s argument: %s",
                            makeOptionString(option), e.getMessage()));
                }

            } else if (option.equals(OPTION_QUERY)) {
                if (ioQueryMode != null)
                    throw new CliArgumentException(String.format(
                            "The options %s and %s are mutually exclusive.",
                            makeOptionString(OPTION_IO),
                            makeOptionString(OPTION_QUERY)));

                String[] opts = option.getValues();
                if (opts.length < 2)
                    throw new CliArgumentException(
                            String.format(
                                    "Illegal %s argument: Must specify mode and at least one file.",
                                    makeOptionString(option)));

                QueryMode queryMode = null;
                try {
                    queryMode = QueryMode.forString(opts[0]);
                } catch (RuntimeException e) {
                    throw new CliArgumentException(String.format(
                            "Illegal %s argument: %s",
                            makeOptionString(option), e.getMessage()));
                }

                Set<Path> files = new HashSet<>();
                for (int i = 1; i != opts.length; ++i)
                    files.add(getAndCheckFile(opts[i]));

                queries.add(new SimpleEntry<>(queryMode, files));
            }

            else if (option.equals(OPTION_LOG_CONSOLE))
                logConsole = true;

            else if (option.equals(OPTION_LOG_DEBUG))
                logDebug = true;

            else
                throw new CliArgumentException(String.format(
                        "Unexpected option: '%s'.", option));

        if (NioUtils.checkFile(inputArg, IS_DIRECTORY)) {
            if (workingDir != null)
                throw new CliArgumentException(
                        String.format(
                                "Can't use --%s (-%s) argument if using existing working directory as input.",
                                OPTION_WORKINGDIR.getLongOpt(),
                                OPTION_WORKINGDIR.getOpt()));

            workingDir = inputArg;
            corpus = getWorkingDirFile(Constants.TRAINING_FILE_NAME);
            getWorkingDirFile(Constants.STATUS_FILE_NAME);
        } else {
            if (workingDir == null)
                workingDir = Paths.get(inputArg + Constants.WORKING_DIR_SUFFIX);
            if (NioUtils.checkFile(workingDir, EXISTS, IS_NO_DIRECTORY))
                throw new IOException(
                        String.format(
                                "Working directory '%s' already exists but is not a directory.",
                                workingDir));

            corpus = inputArg;
        }

        if (estimators.isEmpty())
            estimators.add(OPTION_ESTIMATOR_ARGUMENTS.values().iterator().next());

        if (ioQueryMode != null && estimators.size() > 1)
            throw new CliArgumentException(String.format(
                    "Can specify at most one estimator if using option %s.",
                    makeOptionString(OPTION_IO)));

        if (trainingOrder == null)
            trainingOrder = calculateTrainingOrder();
        else
            verifyTrainingOrder();

        // Need to create workingDirectory here in order to create Logger for
        // "<workingdir>/log" as soon as possible.
        try {
            Files.createDirectories(workingDir);
        } catch (IOException e) {
            throw new IOException(String.format(
                    "Could not create working directory '%s'.", workingDir), e);
        }
    }

    private String makeOptionString(Option option) {
        return String.format("--%s (-%s)", option.getLongOpt(), option.getOpt());
    }

    private void optionFirstTimeOrFail(Object value,
                                       Option option) {
        if (value != null)
            throw new CliArgumentException(String.format(
                    "Option %s must not be specified more than once.",
                    makeOptionString(option)));
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
                throw new CliArgumentException(f.toString());
            }
        return v;
    }

    private Path getAndCheckFile(String filename) throws IOException {
        Path file = Paths.get(filename);
        if (!NioUtils.checkFile(file, EXISTS, IS_READABLE))
            throw new IOException(String.format(
                    "File %s does not exist or is not readable.", filename));
        return file;
    }

    private Path getWorkingDirFile(String filename) throws IOException {
        Path file = workingDir.resolve(filename);
        if (!NioUtils.checkFile(file, EXISTS, IS_READABLE))
            throw new IOException(String.format(
                    "%s file '%s' does not exist or is not readable.",
                    filename, file));
        return file;
    }

    private int calculateTrainingOrder() throws IOException {
        int maxOrder = 0;
        for (Entry<QueryMode, Set<Path>> entry : queries) {
            QueryMode queryMode = entry.getKey();
            Integer queryOrder = queryMode.getOrder();
            if (queryOrder != null && maxOrder < queryOrder)
                maxOrder = queryOrder;

            Set<Path> files = entry.getValue();
            for (Path file : files)
                try (BufferedReader reader = Files.newBufferedReader(file,
                        Constants.CHARSET)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String trimmed = line.trim();
                        if (trimmed.isEmpty() || trimmed.charAt(0) == '#')
                            continue;

                        List<String> split = StringUtils.splitAtChar(trimmed,
                                ' ');
                        int lineOrder = split.size();
                        if (maxOrder < lineOrder)
                            maxOrder = lineOrder;
                    }
                } catch (IOException e) {
                    throw new IOException(String.format(
                            "Error reading file '%s'.", file), e);
                }
        }

        if (maxOrder == 0)
            maxOrder = 5;

        return maxOrder;
    }

    private void verifyTrainingOrder() {
        for (Entry<QueryMode, Set<Path>> entry : queries) {
            QueryMode queryMode = entry.getKey();
            Integer queryOrder = queryMode.getOrder();
            if (queryOrder != null && queryOrder > trainingOrder)
                throw new CliArgumentException(
                        String.format(
                                "Given order for query '%s' is higher than given training order '%d'.",
                                queryMode, trainingOrder));
        }
    }

    @Override
    protected void configureLogging() {
        super.configureLogging();

        LOGGING_HELPER.addFileAppender(
                workingDir.resolve(Constants.LOCAL_LOG_FILE_NAME), "FileLocal",
                true);

        if (logConsole) {
            LOGGING_HELPER.addConsoleAppender(Target.SYSTEM_ERR);
            // Stop clash of Log Messages with CondoleOutputter's Ansi Control Codes.
            OUTPUT.disableAnsi();
        }

        if (logDebug)
            LOGGING_HELPER.setLogLevel(Level.DEBUG);
    }

    @Override
    protected void exec() throws Exception {
        logFields();

        Glmtk glmtk = new Glmtk(corpus, workingDir);

        boolean needPos = false;

        ProbMode probMode = ProbMode.MARG;

        Set<Pattern> neededPatterns = new HashSet<>();
        for (Estimator estimator : estimators) {
            neededPatterns.addAll(Patterns.getUsedPatterns(trainingOrder,
                    estimator, probMode));
            if (needPos)
                neededPatterns.addAll(Patterns.getPosPatterns(neededPatterns));
        }

        glmtk.count(neededPatterns);

        for (Entry<QueryMode, Set<Path>> entry : queries) {
            QueryMode queryMode = entry.getKey();
            Set<Path> files = entry.getValue();

            for (Path file : files) {
                CountCache countCache = glmtk.provideQueryCache(file,
                        neededPatterns);

                for (Estimator estimator : estimators)
                    glmtk.runQueriesOnFile(queryMode, file, estimator,
                            probMode, countCache);
            }
        }

        if (ioQueryMode != null) {
            Estimator estimator = estimators.iterator().next();
            CountCache countCache = glmtk.createCountCache(neededPatterns);
            glmtk.runQueriesOnInputStream(ioQueryMode, System.in, System.out,
                    estimator, probMode, countCache);
        }

        StatisticalNumberHelper.print();
    }

    private void logFields() {
        LOGGER.debug("GlmtkExecutable Fields %s", StringUtils.repeat("-",
                80 - "GlmtkExecutable Fields ".length()));
        LOGGER.debug("Corpus:        %s", corpus);
        LOGGER.debug("WorkingDir:    %s", workingDir);
        LOGGER.debug("TrainingOrder: %s", trainingOrder);
        LOGGER.debug("Estimators:    %s", estimators);
        LOGGER.debug("ioQueryMode:   %s", ioQueryMode);
        LOGGER.debug("queries:       %s", queries);
        LOGGER.debug("LogConsole:    %s", logConsole);
        LOGGER.debug("LogDebug:      %s", logDebug);
    }
}
