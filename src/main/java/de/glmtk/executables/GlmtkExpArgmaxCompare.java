package de.glmtk.executables;

import static de.glmtk.common.Output.OUTPUT;
import static de.glmtk.util.NioUtils.CheckFile.EXISTS;
import static de.glmtk.util.NioUtils.CheckFile.IS_DIRECTORY;
import static de.glmtk.util.NioUtils.CheckFile.IS_READABLE;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import de.glmtk.Constants;
import de.glmtk.Glmtk;
import de.glmtk.GlmtkPaths;
import de.glmtk.cache.Cache;
import de.glmtk.cache.CacheSpecification;
import de.glmtk.cache.CacheSpecification.CacheImplementation;
import de.glmtk.cache.CompletionTrieCache;
import de.glmtk.common.Output.Phase;
import de.glmtk.common.Output.Progress;
import de.glmtk.common.Pattern;
import de.glmtk.common.Patterns;
import de.glmtk.common.Status;
import de.glmtk.exceptions.CliArgumentException;
import de.glmtk.logging.Logger;
import de.glmtk.options.BooleanOption;
import de.glmtk.options.PathsOption;
import de.glmtk.options.custom.ArgmaxExecutorOption;
import de.glmtk.options.custom.ArgmaxExecutorsOption;
import de.glmtk.options.custom.EstimatorsOption;
import de.glmtk.querying.argmax.ArgmaxQueryCacheCreator;
import de.glmtk.querying.argmax.ArgmaxQueryExecutor;
import de.glmtk.querying.argmax.ArgmaxQueryExecutor.ArgmaxResult;
import de.glmtk.querying.estimator.Estimator;
import de.glmtk.querying.estimator.weightedsum.WeightedSumEstimator;
import de.glmtk.util.HashUtils;
import de.glmtk.util.NioUtils;
import de.glmtk.util.StringUtils;

public class GlmtkExpArgmaxCompare extends Executable {
    private static final Logger LOGGER = Logger.get(GlmtkExpArgmaxCompare.class);

    public static void main(String[] args) {
        new GlmtkExpArgmaxCompare().run(args);
    }

    private ArgmaxExecutorsOption optionArgmaxExecutors;
    private EstimatorsOption optionEstimators;
    private PathsOption optionQuery;
    private BooleanOption optionRandomAccess;
    private BooleanOption optionNoQueryCache;

    private Path corpus = null;
    private Path workingDir = null;
    private Set<String> executors = new LinkedHashSet<>();
    private Set<WeightedSumEstimator> estimators = new LinkedHashSet<>();
    private Set<Path> queries = new LinkedHashSet<>();
    private Boolean randomAccess = null;
    private Boolean noQueryCache = null;

    @Override
    protected String getExecutableName() {
        return "glmtk-exp-argmaxcompare";
    }

    @Override
    protected void registerOptions() {
        optionArgmaxExecutors = new ArgmaxExecutorsOption("a",
                "argmax-executor",
                "Executors to compare. Can be specified multiple times.");
        optionEstimators = new EstimatorsOption("e", "estimator",
                "Estimators to use. Only weighted sum Estimators are allowed. "
                        + "Can be specified multiple times.").needWeightedSum();
        optionQuery = new PathsOption("q", "query",
                "Query the given files. Can be specified multiple times.").constrainMustExist().constrainFiles();
        optionRandomAccess = new BooleanOption("r", "random-access",
                "Use a HashMap baseed cache for any random access caches "
                        + "instead of default CompletionTrie based cache.");
        optionNoQueryCache = new BooleanOption("c", "no-querycache",
                "Do not create QueryCache.");

        optionManager.register(optionArgmaxExecutors, optionEstimators,
                optionQuery, optionRandomAccess, optionNoQueryCache);
    }

    @Override
    protected String getHelpHeader() {
        return "Performs comparision of argmax query executors.";
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
        // TODO: check existance and readability of corpus file.
        if (!NioUtils.checkFile(workingDir, IS_DIRECTORY))
            throw new IOException(String.format(
                    "Working directory '%s' is not a directory.", workingDir));
        if (!NioUtils.checkFile(workingDir, EXISTS, IS_READABLE))
            throw new IOException(
                    String.format(
                            "Working directory '%s' does not exist or is not readable.",
                            workingDir));

        if (executors.isEmpty())
            throw new CliArgumentException(
                    String.format("No executors given, use option %s.",
                            optionArgmaxExecutors));

        if (queries.isEmpty())
            throw new CliArgumentException(String.format(
                    "No files to query given, use option %s.", optionQuery));

        if (randomAccess == null)
            randomAccess = false;

        if (noQueryCache == null)
            noQueryCache = false;
    }

    //    private void parseFlags() throws IOException {
    //        @SuppressWarnings("unchecked")
    //        Iterator<Option> iter = line.iterator();
    //        while (iter.hasNext()) {
    //            Option option = iter.next();
    //
    //            if (option.equals(OPTION_ARGMAX_EXECUTOR))
    //                for (String opt : option.getValues()) {
    //                    opt = opt.toUpperCase();
    //                    if (!OPTION_ARGMAX_EXECUTORS_ARGUMENTS.containsKey(opt))
    //                        throw new CliArgumentException(
    //                                String.format(
    //                                        "Illegal %s argument. Unkown estimators option '%s'. Valid arguments would be: '%s'.",
    //                                        makeOptionString(option),
    //                                        opt,
    //                                        StringUtils.join(
    //                                                OPTION_ARGMAX_EXECUTORS_ARGUMENTS.keySet(),
    //                                                "', '")));
    //                    executors.add(opt);
    //                }
    //
    //            else if (option.equals(OPTION_ESTIMATOR))
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
    //                    if (!(estimator instanceof WeightedSumEstimator))
    //                        throw new CliArgumentException(
    //                                String.format(
    //                                        "Illegal %s argument. Given estimator '%s' is not a weighted sum estimator.",
    //                                        estimator));
    //                    estimators.add((WeightedSumEstimator) estimator);
    //                }
    //
    //            else if (option.equals(OPTION_QUERY))
    //                for (String opt : option.getValues())
    //                    queries.add(getAndCheckFile(opt));
    //
    //            else if (option.equals(OPTION_RANDOM_ACCESS)) {
    //                optionFirstTimeOrFail(randomAccess, option);
    //                randomAccess = true;
    //
    //            } else if (option.equals(OPTION_NO_QUERYCACHE)) {
    //                optionFirstTimeOrFail(noQueryCache, option);
    //                noQueryCache = true;
    //
    //            } else
    //                throw new CliArgumentException(String.format(
    //                        "Unexpected option: '%s'.", option));
    //        }
    //    }

    @Override
    protected void exec() throws Exception {
        logFields();

        Glmtk glmtk = new Glmtk(config, corpus, workingDir);

        int neededOrder = getNeededOrder();

        CacheSpecification cacheSpec = new CacheSpecification();
        cacheSpec.withProgress();
        for (Estimator estimator : estimators)
            cacheSpec.addAll(estimator.getRequiredCache(neededOrder));
        cacheSpec.withCounts(Patterns.getMany("x")); // FIXME: Refactor this
        cacheSpec.withWords(); // FIXE: Refactor this

        Set<Pattern> requiredPatterns = cacheSpec.getRequiredPatterns();
        requiredPatterns.add(Patterns.get("x1111x")); // FIXME: Refactor this

        Status status = glmtk.getStatus();
        GlmtkPaths paths = glmtk.getPaths();

        for (Path queryFile : queries) {
            OUTPUT.printMessage("");
            OUTPUT.printMessage(queryFile + ":");

            // TODO: there really should be an API for the following:
            String hash = HashUtils.generateMd5Hash(queryFile);

            GlmtkPaths queryCachePaths;
            if (noQueryCache)
                queryCachePaths = paths;
            else {
                ArgmaxQueryCacheCreator argmaxQueryCacheCreator = new ArgmaxQueryCacheCreator(
                        config);
                queryCachePaths = argmaxQueryCacheCreator.createQueryCache(
                        "argmax" + hash, queryFile, false, requiredPatterns,
                        status, paths);
            }

            CompletionTrieCache sortedAccessCache = (CompletionTrieCache) cacheSpec.withCacheImplementation(
                    CacheImplementation.COMPLETION_TRIE).build(queryCachePaths);
            Cache randomAccessCache = sortedAccessCache;
            if (randomAccess)
                randomAccessCache = cacheSpec.withCacheImplementation(
                        CacheImplementation.HASH_MAP).build(queryCachePaths);

            for (String executor : executors)
                for (WeightedSumEstimator estimator : estimators) {
                    estimator.setCache(randomAccessCache);
                    ArgmaxQueryExecutor argmaxQueryExecutor = ArgmaxExecutorOption.argmaxQueryExecutorFromString(
                            executor, estimator, randomAccessCache,
                            sortedAccessCache);

                    BigInteger timeSum = BigInteger.ZERO;
                    int n = 0;

                    String type = String.format("%s-%s:", executor,
                            estimator.getName());
                    OUTPUT.beginPhases(type + " Querying...");
                    OUTPUT.setPhase(Phase.QUERYING);
                    Progress progress = OUTPUT.newProgress(NioUtils.calcNumberOfLines(queryFile));
                    try (BufferedReader reader = Files.newBufferedReader(
                            queryFile, Constants.CHARSET);
                            BufferedWriter writer = Files.newBufferedWriter(
                                    Paths.get(queryFile
                                            + "."
                                            + type.substring(0,
                                                    type.length() - 1)),
                                                    Constants.CHARSET)) {

                        String line;
                        int i = 0;
                        while ((line = reader.readLine()) != null) {
                            int lastSpacePos = line.lastIndexOf(' ');
                            String history = line.substring(0, lastSpacePos);
                            String sequence = line.substring(lastSpacePos);
                            long timeBefore = System.nanoTime();
                            List<ArgmaxResult> argmaxResults = argmaxQueryExecutor.queryArgmax(
                                    history, 5);
                            long timeAfter = System.nanoTime();

                            writer.append(String.format("%s : %s ", history,
                                    sequence));
                            for (ArgmaxResult a : argmaxResults)
                                writer.append(String.format("[%s-%e]",
                                        a.getSequence(), a.getProbability()));
                            writer.append('\n');

                            timeSum = timeSum.add(BigInteger.valueOf(timeAfter
                                    - timeBefore));
                            progress.increase(1);
                            ++n;
                        }
                    }
                    OUTPUT.endPhases(type);

                    BigInteger timePerArgmax = timeSum.divide(BigInteger.valueOf(n));
                    OUTPUT.printMessage(String.format(
                            "- Average prediciton time: %.3fms",
                            (timePerArgmax.floatValue() / 1000 / 1000)));
                }
        }
    }

    private int getNeededOrder() throws IOException {
        int neededOrder = 0;
        for (Path queryFile : queries)
            try (BufferedReader reader = Files.newBufferedReader(queryFile,
                    Constants.CHARSET)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    int order = StringUtils.split(line, ' ').size();
                    if (neededOrder < order)
                        neededOrder = order;
                }
            }
        return neededOrder;
    }

    private void logFields() {
        LOGGER.debug("%s %s", getExecutableName(), StringUtils.repeat("-",
                80 - getExecutableName().length()));
        LOGGER.debug("Corpus:     %s", corpus);
        LOGGER.debug("WorkingDir: %s", workingDir);
        LOGGER.debug("Executors:  %s", executors);
        LOGGER.debug("Estimators: %s", estimators);
        LOGGER.debug("Queries:    %s", queries);
    }
}
