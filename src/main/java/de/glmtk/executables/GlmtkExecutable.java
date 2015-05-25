/*
 * Generalized Language Modeling Toolkit (GLMTK)
 * 
 * Copyright (C) 2014-2015 Lukas Schmelzeisen
 * 
 * GLMTK is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * GLMTK is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * GLMTK. If not, see <http://www.gnu.org/licenses/>.
 * 
 * See the AUTHORS file for contributors.
 */

package de.glmtk.executables;

import static de.glmtk.common.Output.OUTPUT;
import static de.glmtk.util.LoggingHelper.LOGGING_HELPER;
import static de.glmtk.util.NioUtils.CheckFile.EXISTS;
import static de.glmtk.util.NioUtils.CheckFile.IS_DIRECTORY;
import static de.glmtk.util.NioUtils.CheckFile.IS_NO_DIRECTORY;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.appender.ConsoleAppender.Target;

import de.glmtk.Constants;
import de.glmtk.Glmtk;
import de.glmtk.GlmtkPaths;
import de.glmtk.cache.Cache;
import de.glmtk.cache.CacheSpecification;
import de.glmtk.common.Pattern;
import de.glmtk.common.PatternElem;
import de.glmtk.common.Patterns;
import de.glmtk.common.ProbMode;
import de.glmtk.counting.Tagger;
import de.glmtk.exceptions.CliArgumentException;
import de.glmtk.exceptions.FileFormatException;
import de.glmtk.logging.Logger;
import de.glmtk.options.IntegerOption;
import de.glmtk.options.PathOption;
import de.glmtk.options.custom.EstimatorsOption;
import de.glmtk.options.custom.QueryModeFilesOption;
import de.glmtk.options.custom.QueryModeOption;
import de.glmtk.querying.estimator.Estimator;
import de.glmtk.querying.estimator.Estimators;
import de.glmtk.querying.probability.QueryMode;
import de.glmtk.util.NioUtils;
import de.glmtk.util.StatisticalNumberHelper;
import de.glmtk.util.StringUtils;

public class GlmtkExecutable extends Executable {
    private static final Logger LOGGER = Logger.get(GlmtkExecutable.class);

    public static void main(String[] args) {
        new GlmtkExecutable().run(args);
    }

    private PathOption optionWorkingDir;
    private IntegerOption optionTrainingOrder;
    private EstimatorsOption optionEstimators;
    private QueryModeOption optionIo;
    private QueryModeFilesOption optionQuery;

    private Path corpus = null;
    private Path workingDir = null;
    private Integer trainingOrder = null;
    private Set<Estimator> estimators = new LinkedHashSet<>();
    private QueryMode ioQueryMode = null;
    private List<Entry<QueryMode, Set<Path>>> queries = new LinkedList<>();
    private boolean logConsole = false;
    private boolean logDebug = false;

    @Override
    protected String getExecutableName() {
        return "glmtk";
    }

    @Override
    protected void registerOptions() {
        optionWorkingDir = new PathOption("w", "workingdir",
                "Working directory.").constrainMayExist().constrainDirectory();
        optionTrainingOrder = new IntegerOption("n", "training-order",
                "Order to learn for training.").constainPositive().contrainNotZero();
        optionEstimators = new EstimatorsOption("e", "estimator",
                "Estimators to learn for and query with.");
        optionIo = new QueryModeOption("i", "io",
                "Takes queries from standard input with given mode.");
        optionQuery = new QueryModeFilesOption("q", "query",
                "Query the given files with given mode.");

        optionManager.register(optionWorkingDir, optionTrainingOrder,
                optionEstimators, optionIo, optionQuery);
    }

    @Override
    protected String getHelpHeader() {
        return "Invokes the Generalized Language Model Toolkit.";
    }

    @Override
    protected String getHelpFooter() {
        return null;
    }

    @Override
    protected void parseOptions(String[] args) throws Exception {
        super.parseOptions(args);

        corpus = parseInputArg();

        if (NioUtils.checkFile(corpus, IS_DIRECTORY)) {
            if (workingDir != null)
                throw new CliArgumentException(
                        String.format(
                                "Can't use %s option if using existing working directory as input.",
                                optionWorkingDir));

            workingDir = corpus;
            corpus = getWorkingDirFile(workingDir, Constants.TRAINING_FILE_NAME);
            getWorkingDirFile(workingDir, Constants.STATUS_FILE_NAME);
        } else {
            if (workingDir == null)
                workingDir = Paths.get(corpus + Constants.WORKING_DIR_SUFFIX);
            if (NioUtils.checkFile(workingDir, EXISTS, IS_NO_DIRECTORY))
                throw new IOException(
                        String.format(
                                "Working directory '%s' already exists but is not a directory.",
                                workingDir));
        }

        checkCorpusForReservedSymbols();

        if (estimators.isEmpty())
            estimators.add(Estimators.FAST_MLE);

        if (ioQueryMode != null && estimators.size() > 1)
            throw new CliArgumentException(String.format(
                    "Can specify at most one estimator if using option %s.",
                    optionIo));

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

    //    private void parseFlags() throws IOException {
    //        @SuppressWarnings("unchecked")
    //        Iterator<Option> iter = line.iterator();
    //        while (iter.hasNext()) {
    //            Option option = iter.next();
    //
    //            if (option.equals(OPTION_WORKINGDIR)) {
    //                optionFirstTimeOrFail(workingDir, option);
    //                workingDir = Paths.get(option.getValue());
    //
    //            } else if (option.equals(OPTION_TRAINING_ORDER)) {
    //                optionFirstTimeOrFail(trainingOrder, option);
    //                trainingOrder = optionPositiveIntOrFail(option.getValue(),
    //                        false, "Illegal %s argument", makeOptionString(option));
    //
    //            } else if (option.equals(OPTION_ESTIMATOR))
    //                for (String opt : option.getValues()) {
    //                    Estimator estimator = OPTION_ESTIMATOR_ARGUMENTS.get(opt.toUpperCase());
    //                    if (estimator == null)
    //                        throw new CliArgumentException(
    //                                String.format(
    //                                        "Illegal %s argument. Unkown estimators option '%s'. Valid arguments would be: '%s'.",
    //                                        makeOptionString(option),
    //                                        opt,
    //                                        StringUtils.join(
    //                                                OPTION_ESTIMATOR_ARGUMENTS.keySet(),
    //                                                "', '")));
    //                    estimators.add(estimator);
    //                }
    //
    //            else if (option.equals(OPTION_IO)) {
    //                optionFirstTimeOrFail(ioQueryMode, option);
    //                if (!queries.isEmpty())
    //                    throw new CliArgumentException(String.format(
    //                            "The options %s and %s are mutually exclusive.",
    //                            makeOptionString(OPTION_IO),
    //                            makeOptionString(OPTION_QUERY)));
    //
    //                try {
    //                    ioQueryMode = QueryMode.forString(option.getValue());
    //                } catch (RuntimeException e) {
    //                    throw new CliArgumentException(String.format(
    //                            "Illegal %s argument: %s",
    //                            makeOptionString(option), e.getMessage()));
    //                }
    //
    //            } else if (option.equals(OPTION_QUERY)) {
    //                if (ioQueryMode != null)
    //                    throw new CliArgumentException(String.format(
    //                            "The options %s and %s are mutually exclusive.",
    //                            makeOptionString(OPTION_IO),
    //                            makeOptionString(OPTION_QUERY)));
    //
    //                String[] opts = option.getValues();
    //                if (opts.length < 2)
    //                    throw new CliArgumentException(
    //                            String.format(
    //                                    "Illegal %s argument: Must specify mode and at least one file.",
    //                                    makeOptionString(option)));
    //
    //                QueryMode queryMode = null;
    //                try {
    //                    queryMode = QueryMode.forString(opts[0]);
    //                } catch (RuntimeException e) {
    //                    throw new CliArgumentException(String.format(
    //                            "Illegal %s argument: %s",
    //                            makeOptionString(option), e.getMessage()));
    //                }
    //
    //                Set<Path> files = new HashSet<>();
    //                for (int i = 1; i != opts.length; ++i)
    //                    files.add(getAndCheckFile(opts[i]));
    //
    //                queries.add(new SimpleEntry<>(queryMode, files));
    //            }
    //
    //            else if (option.equals(OPTION_LOG_CONSOLE))
    //                logConsole = true;
    //
    //            else if (option.equals(OPTION_LOG_DEBUG))
    //                logDebug = true;
    //
    //            else
    //                throw new CliArgumentException(String.format(
    //                        "Unexpected option: '%s'.", option));
    //        }
    //    }

    private void checkCorpusForReservedSymbols() throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(corpus,
                Constants.CHARSET)) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                ++lineNo;
                checkLineForReservedSymbol(line, lineNo, PatternElem.SKP_WORD);
                checkLineForReservedSymbol(line, lineNo, PatternElem.WSKP_WORD);
                checkLineForReservedSymbol(line, lineNo,
                        Character.toString(Tagger.POS_SEPARATOR));
            }
        }
    }

    private void checkLineForReservedSymbol(String line,
                                            int lineNo,
                                            String symbol) {
        if (line.contains(symbol))
            throw new FileFormatException(line, lineNo, corpus, "training",
                    "Training file contains reserved symbol '%s'.", symbol);
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

                        List<String> split = StringUtils.split(trimmed, ' ');
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

        if (logDebug
                && LOGGING_HELPER.getLogLevel().isMoreSpecificThan(Level.DEBUG))
            LOGGING_HELPER.setLogLevel(Level.DEBUG);
    }

    /**
     * the real entry point to the toolkit.
     */
    @Override
    protected void exec() throws Exception {
        logFields();

        Glmtk glmtk = new Glmtk(config, corpus, workingDir);

        boolean needPos = false;

        // Set up Estimators
        ProbMode probMode = ProbMode.MARG;
        for (Estimator estimator : estimators)
            estimator.setProbMode(probMode);

        CacheSpecification cacheBuilder = new CacheSpecification();
        for (Estimator estimator : estimators)
            cacheBuilder.addAll(estimator.getRequiredCache(trainingOrder));
        // FIXME: Refactor this!
        cacheBuilder.withCounts(Patterns.getMany("x"));

        Set<Pattern> requiredPatterns = cacheBuilder.getRequiredPatterns();
        if (needPos)
            requiredPatterns.addAll(Patterns.getPosPatterns(requiredPatterns));
        // FIXME: Refactor this!
        requiredPatterns.add(Patterns.get("x1111x"));

        glmtk.count(requiredPatterns);

        for (Entry<QueryMode, Set<Path>> entry : queries) {
            QueryMode queryMode = entry.getKey();
            Set<Path> files = entry.getValue();

            for (Path file : files) {
                GlmtkPaths queryCache = glmtk.provideQueryCache(file,
                        requiredPatterns);
                Cache cache = cacheBuilder.build(queryCache);

                for (Estimator estimator : estimators) {
                    estimator.setCache(cache);
                    glmtk.queryFile(queryMode, estimator, trainingOrder, file);
                }
            }
        }

        if (ioQueryMode != null) {
            Cache cache = cacheBuilder.withProgress().build(glmtk.getPaths());
            Estimator estimator = estimators.iterator().next();
            estimator.setCache(cache);
            glmtk.queryStream(ioQueryMode, estimator, trainingOrder, System.in,
                    System.out);
        }

        StatisticalNumberHelper.print();
    }

    private void logFields() {
        LOGGER.debug("%s %s", getExecutableName(), StringUtils.repeat("-",
                80 - getExecutableName().length()));
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
