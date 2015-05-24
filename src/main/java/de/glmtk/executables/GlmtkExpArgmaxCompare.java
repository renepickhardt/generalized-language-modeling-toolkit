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
import java.util.Arrays;
import java.util.Formatter;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.cli.Option;

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
import de.glmtk.exceptions.SwitchCaseNotImplementedException;
import de.glmtk.logging.Logger;
import de.glmtk.querying.argmax.ArgmaxQueryCacheCreator;
import de.glmtk.querying.argmax.ArgmaxQueryExecutor;
import de.glmtk.querying.argmax.ArgmaxQueryExecutor.ArgmaxResult;
import de.glmtk.querying.argmax.BeamSearchArgmaxQueryExecutor;
import de.glmtk.querying.argmax.NoRandomAccessArgmaxQueryExecutor;
import de.glmtk.querying.argmax.ThresholdArgmaxQueryExecutor;
import de.glmtk.querying.argmax.TrivialArgmaxQueryExecutor;
import de.glmtk.querying.estimator.Estimator;
import de.glmtk.querying.estimator.weightedsum.WeightedSumEstimator;
import de.glmtk.util.HashUtils;
import de.glmtk.util.NioUtils;
import de.glmtk.util.StringUtils;

public class GlmtkExpArgmaxCompare extends Executable {
    private static final Logger LOGGER = Logger.get(GlmtkExpArgmaxCompare.class);

    private static final Option OPTION_HELP;
    private static final Option OPTION_VERSION;
    private static final Option OPTION_ARGMAX_EXECUTOR;
    private static final Option OPTION_ESTIMATOR;
    private static final Option OPTION_QUERY;
    private static final Option OPTION_RANDOM_ACCESS;
    private static final Option OPTION_NO_QUERYCACHE;

    private static final List<Option> OPTIONS;

    static {
        OPTION_HELP = new Option(OPTION_HELP_SHORT, OPTION_HELP_LONG, false,
                "Print this message.");

        OPTION_VERSION = new Option(OPTION_VERSION_SHORT, OPTION_VERSION_LONG,
                false, "Print the version information and exit.");

        OPTION_ARGMAX_EXECUTOR = new Option("a", "argmax-executor", true,
                "Executors to compare. Can be specified multiple times.");
        OPTION_ARGMAX_EXECUTOR.setArgName("EXECUTOR...");
        OPTION_ARGMAX_EXECUTOR.setArgs(Option.UNLIMITED_VALUES);

        OPTION_ESTIMATOR = new Option(
                "e",
                "estimator",
                true,
                "Estimators to use. Only weighted sum Estimators are allowed. Can be specified multiple times.");
        OPTION_ESTIMATOR.setArgName("ESTIMATOR...");
        OPTION_ESTIMATOR.setArgs(Option.UNLIMITED_VALUES);

        OPTION_QUERY = new Option("q", "query", true,
                "Query the given files. Can be specified multiple times.");
        OPTION_QUERY.setArgName("FILE...");
        OPTION_QUERY.setArgs(Option.UNLIMITED_VALUES);

        OPTION_RANDOM_ACCESS = new Option(
                "r",
                "random-access",
                false,
                "Use a HashMap baseed cache for any random access caches instead of default CompletionTrie based cache.");

        OPTION_NO_QUERYCACHE = new Option("c", "no-querycache", false,
                "Do not create QueryCache.");

        OPTIONS = Arrays.asList(OPTION_HELP, OPTION_VERSION,
                OPTION_ARGMAX_EXECUTOR, OPTION_ESTIMATOR, OPTION_QUERY,
                OPTION_RANDOM_ACCESS, OPTION_NO_QUERYCACHE);
    }

    private static final Map<String, String> OPTION_ARGMAX_EXECUTORS_ARGUMENTS;
    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("TA", "TopK Treshold Algorithm");
        m.put("NRA", "TopK No Random Access");
        m.put("BEAM", "Beam Search");
        m.put("SMPL", "Trivial");
        OPTION_ARGMAX_EXECUTORS_ARGUMENTS = m;
    }

    private static ArgmaxQueryExecutor argmaxQueryExecutorFromString(String executor,
                                                                     WeightedSumEstimator estimator,
                                                                     Cache randomAccessCache,
                                                                     CompletionTrieCache sortedAccessCache) {
        switch (executor) {
            case "TA":
                return new ThresholdArgmaxQueryExecutor(estimator,
                        randomAccessCache, sortedAccessCache);

            case "NRA":
                return new NoRandomAccessArgmaxQueryExecutor(estimator,
                        sortedAccessCache);

            case "BEAM":
                return new BeamSearchArgmaxQueryExecutor(estimator,
                        sortedAccessCache);

            case "SMPL":
                return new TrivialArgmaxQueryExecutor(estimator,
                        randomAccessCache);

            default:
                throw new SwitchCaseNotImplementedException();
        }
    }

    public static void main(String[] args) {
        new GlmtkExpArgmaxCompare().run(args);
    }

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
    protected List<Option> getOptions() {
        return OPTIONS;
    }

    @Override
    protected String getHelpHeader() {
        try (Formatter f = new Formatter()) {
            f.format("%s <INPUT> [<OPTOIN...>]%n", getExecutableName());
            f.format("Performs comparision of argmax query executors.%n");

            f.format("%nMandatory arguments to long options are mandatory for short options too.%n");

            return f.toString();
        }
    }

    @Override
    protected String getHelpFooter() {
        try (Formatter f = new Formatter()) {
            f.format("%nWhere <EXECUTOR> may be any of:%n");
            for (Entry<String, String> executor : OPTION_ARGMAX_EXECUTORS_ARGUMENTS.entrySet())
                f.format("  * %-5s %s%n", executor.getKey(),
                        executor.getValue());

            f.format("%nWhere <ESTIMATOR> may be any of:%n");
            for (Entry<String, Estimator> arg : OPTION_ESTIMATOR_ARGUMENTS.entrySet()) {
                if (!(arg.getValue() instanceof WeightedSumEstimator))
                    continue;
                f.format("  * %-5s  %s%n", arg.getKey(),
                        arg.getValue().getName());
            }

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
            throw new CliArgumentException(String.format(
                    "No executors given (%s).",
                    makeOptionString(OPTION_ARGMAX_EXECUTOR)));

        if (queries.isEmpty())
            throw new CliArgumentException(String.format(
                    "No files to query given (%s).",
                    makeOptionString(OPTION_QUERY)));

        if (randomAccess == null)
            randomAccess = false;

        if (noQueryCache == null)
            noQueryCache = false;
    }

    private void parseFlags() throws IOException {
        @SuppressWarnings("unchecked")
        Iterator<Option> iter = line.iterator();
        while (iter.hasNext()) {
            Option option = iter.next();

            if (option.equals(OPTION_ARGMAX_EXECUTOR))
                for (String opt : option.getValues()) {
                    opt = opt.toUpperCase();
                    if (!OPTION_ARGMAX_EXECUTORS_ARGUMENTS.containsKey(opt))
                        throw new CliArgumentException(
                                String.format(
                                        "Illegal %s argument. Unkown estimators option '%s'. Valid arguments would be: '%s'.",
                                        makeOptionString(option),
                                        opt,
                                        StringUtils.join(
                                                OPTION_ARGMAX_EXECUTORS_ARGUMENTS.keySet(),
                                                "', '")));
                    executors.add(opt);
                }

            else if (option.equals(OPTION_ESTIMATOR))
                for (String opt : option.getValues()) {
                    Estimator estimator = OPTION_ESTIMATOR_ARGUMENTS.get(opt.toUpperCase());
                    if (estimator == null)
                        throw new CliArgumentException(
                                String.format(
                                        "Illegal %s argument. Unkown estimators option '%s'. Valid arguments would be: '%s'.",
                                        makeOptionString(option),
                                        opt,
                                        StringUtils.join(
                                                OPTION_ESTIMATOR_ARGUMENTS.keySet(),
                                                "', '")));
                    if (!(estimator instanceof WeightedSumEstimator))
                        throw new CliArgumentException(
                                String.format(
                                        "Illegal %s argument. Given estimator '%s' is not a weighted sum estimator.",
                                        estimator));
                    estimators.add((WeightedSumEstimator) estimator);
                }

            else if (option.equals(OPTION_QUERY))
                for (String opt : option.getValues())
                    queries.add(getAndCheckFile(opt));

            else if (option.equals(OPTION_RANDOM_ACCESS)) {
                optionFirstTimeOrFail(randomAccess, option);
                randomAccess = true;

            } else if (option.equals(OPTION_NO_QUERYCACHE)) {
                optionFirstTimeOrFail(noQueryCache, option);
                noQueryCache = true;

            } else
                throw new CliArgumentException(String.format(
                        "Unexpected option: '%s'.", option));
        }
    }

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
                    ArgmaxQueryExecutor argmaxQueryExecutor = argmaxQueryExecutorFromString(
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
