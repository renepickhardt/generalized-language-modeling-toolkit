package de.glmtk.executables;

import static com.google.common.collect.Sets.newLinkedHashSet;
import static de.glmtk.output.Output.println;
import static de.glmtk.util.NioUtils.countNumberOfLines;
import static de.glmtk.util.StringUtils.repeat;
import static java.lang.String.format;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.newBufferedWriter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.glmtk.Constants;
import de.glmtk.Glmtk;
import de.glmtk.GlmtkPaths;
import de.glmtk.cache.Cache;
import de.glmtk.cache.CacheSpecification;
import de.glmtk.cache.CacheSpecification.CacheImplementation;
import de.glmtk.cache.CompletionTrieCache;
import de.glmtk.common.Pattern;
import de.glmtk.common.Patterns;
import de.glmtk.exceptions.CliArgumentException;
import de.glmtk.logging.Logger;
import de.glmtk.options.BooleanOption;
import de.glmtk.options.IntegerOption;
import de.glmtk.options.PathOption;
import de.glmtk.options.PathsOption;
import de.glmtk.options.custom.ArgmaxExecutorOption;
import de.glmtk.options.custom.ArgmaxExecutorsOption;
import de.glmtk.options.custom.CorpusOption;
import de.glmtk.options.custom.EstimatorsOption;
import de.glmtk.output.ProgressBar;
import de.glmtk.querying.argmax.ArgmaxQueryExecutor;
import de.glmtk.querying.argmax.ArgmaxQueryExecutor.ArgmaxResult;
import de.glmtk.querying.argmax.NoRandomAccessArgmaxQueryExecutor;
import de.glmtk.querying.argmax.ThresholdArgmaxQueryExecutor;
import de.glmtk.querying.estimator.Estimator;
import de.glmtk.querying.estimator.weightedsum.WeightedSumEstimator;
import de.glmtk.util.StringUtils;

public class GlmtkExpArgmaxCompare extends Executable {
    private static final Logger LOGGER = Logger.get(GlmtkExpArgmaxCompare.class);

    public static void main(String[] args) {
        new GlmtkExpArgmaxCompare().run(args);
    }

    private CorpusOption optionCorpus;
    private ArgmaxExecutorsOption optionArgmaxExecutors;
    private EstimatorsOption optionEstimators;
    private PathsOption optionQuery;
    private IntegerOption optionRuns;
    private IntegerOption optionLimit;
    private BooleanOption optionRandomAccess;
    private BooleanOption optionNoQueryCache;
    private PathOption optionResultsDir;
    private PathOption optionOutputDir;

    private Path corpus;
    private Path workingDir;
    private Set<String> executors;
    private Set<WeightedSumEstimator> estimators;
    private Set<Path> queries;
    private Integer times;
    private Integer limit;
    private Boolean randomAccess;
    private Boolean noQueryCache;
    private Path resultsDir;
    private Path outputDir;
    private ProgressBar progressBar;

    private BufferedWriter writerKeystrokes = null;
    private BufferedWriter writerTimeKeystrokes = null;
    private List<BufferedWriter> writersTime = null;
    private List<BufferedWriter> writersNumSortedAccesses = null;
    private List<BufferedWriter> writersNumRandomAccesses = null;
    private List<BufferedWriter> writersProbabilities = null;

    @Override
    protected String getExecutableName() {
        return "glmtk-exp-argmaxcompare";
    }

    @Override
    protected void registerOptions() {
        optionCorpus = new CorpusOption(null, "corpus",
                "Give corpus and maybe working directory.");
        optionArgmaxExecutors = new ArgmaxExecutorsOption("a",
                "argmax-executor",
                "Executors to compare. Can be specified multiple times.");
        optionEstimators = new EstimatorsOption("e", "estimator",
                "Estimators to use. Only weighted sum Estimators are allowed. "
                        + "Can be specified multiple times.").needWeightedSum();
        optionQuery = new PathsOption("q", "query",
                "Query the given files. Can be specified multiple times.").requireMustExist().requireFiles();
        optionRuns = new IntegerOption("N", "num-runs",
                "Number of times to run. Default: 1.").defaultValue(1).requirePositive().requireNotZero();
        optionLimit = new IntegerOption("k", "limit",
                "Compute argmax with limits from 1 to this.").defaultValue(1).requirePositive().requireNotZero();
        optionRandomAccess = new BooleanOption("r", "random-access",
                "Use a HashMap baseed cache for any random access caches "
                        + "instead of default CompletionTrie based cache.");
        optionNoQueryCache = new BooleanOption("c", "no-querycache",
                "Do not create QueryCache.");
        optionResultsDir = new PathOption("d", "results-dir",
                "Directory to store all results like times.").requireMayExist().requireDirectory();
        optionOutputDir = new PathOption("o", "output-dir",
                "Directory to store all output, that is argmax results.").requireMayExist().requireDirectory();

        commandLine.inputArgs(optionCorpus);
        commandLine.options(optionArgmaxExecutors, optionEstimators,
                optionQuery, optionRuns, optionLimit, optionRandomAccess,
                optionNoQueryCache, optionResultsDir, optionOutputDir);
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

        if (!optionCorpus.wasGiven())
            throw new CliArgumentException("%s missing.", optionCorpus);
        corpus = optionCorpus.getCorpus();
        workingDir = optionCorpus.getWorkingDir();

        executors = newLinkedHashSet(optionArgmaxExecutors.getArgmaxExecutors());
        if (executors.isEmpty())
            throw new CliArgumentException(String.format(
                    "No executors given, use %s.", optionArgmaxExecutors));

        @SuppressWarnings({"rawtypes", "unchecked"})
        List<WeightedSumEstimator> list = (List) optionEstimators.getEstimators();
        estimators = newLinkedHashSet(list);
        if (estimators.isEmpty())
            throw new CliArgumentException("No esimators given, use %s.",
                    optionEstimators);

        queries = newLinkedHashSet(optionQuery.getPaths());
        if (queries.isEmpty())
            throw new CliArgumentException("No files to query given, use %s.",
                    optionQuery);

        times = optionRuns.getInt();
        limit = optionLimit.getInt();
        randomAccess = optionRandomAccess.getBoolean();
        noQueryCache = optionNoQueryCache.getBoolean();
        resultsDir = optionResultsDir.getPath();
        outputDir = optionOutputDir.getPath();
    }

    @Override
    protected void exec() throws Exception {
        logFields();

        List<String> phases = new ArrayList<>(executors.size()
                * estimators.size());
        for (String executor : executors)
            for (Estimator estimator : estimators)
                phases.add(executor + "-" + estimator.getName());
        progressBar = new ProgressBar(phases);

        Glmtk glmtk = new Glmtk(config, corpus, workingDir);

        int neededOrder = getNeededOrder();

        CacheSpecification cacheSpec = new CacheSpecification();
        cacheSpec.withProgress();
        for (Estimator estimator : estimators)
            cacheSpec.addAll(estimator.getRequiredCache(neededOrder));
        cacheSpec.withCounts(Patterns.getMany("x")); // FIXME: Refactor this
        cacheSpec.withWords(); // FIXME: Refactor this

        Set<Pattern> requiredPatterns = cacheSpec.getRequiredPatterns();
        requiredPatterns.add(Patterns.get("x1111x")); // FIXME: Refactor this

        GlmtkPaths paths = glmtk.getPaths();

        if (resultsDir != null)
            createDirectories(resultsDir);
        if (outputDir != null)
            createDirectories(outputDir);

        GlmtkPaths queryCachePaths = paths;
        CompletionTrieCache sortedAccessCache = null;
        Cache randomAccessCache = null;
        if (noQueryCache) {
            sortedAccessCache = (CompletionTrieCache) cacheSpec.withCacheImplementation(
                    CacheImplementation.COMPLETION_TRIE).build(queryCachePaths);
            randomAccessCache = sortedAccessCache;
            if (randomAccess)
                randomAccessCache = cacheSpec.withCacheImplementation(
                        CacheImplementation.HASH_MAP).build(queryCachePaths);
        }

        for (Path queryFile : queries) {
            println();
            println(queryFile + ":");

            if (!noQueryCache) {
                queryCachePaths = glmtk.provideArgmaxQueryCache(queryFile,
                        requiredPatterns);
                sortedAccessCache = (CompletionTrieCache) cacheSpec.withCacheImplementation(
                        CacheImplementation.COMPLETION_TRIE).build(
                                queryCachePaths);
                randomAccessCache = sortedAccessCache;
                if (randomAccess)
                    randomAccessCache = cacheSpec.withCacheImplementation(
                            CacheImplementation.HASH_MAP).build(queryCachePaths);
            }

            Iterator<String> phaseIter = phases.iterator();
            for (String executor : executors)
                for (WeightedSumEstimator estimator : estimators) {
                    int numLines = countNumberOfLines(queryFile);
                    String phase = phaseIter.next();
                    progressBar.setPhase(phase, numLines * (times + 1));

                    estimator.setCache(randomAccessCache);
                    ArgmaxQueryExecutor argmaxQueryExecutor = ArgmaxExecutorOption.argmaxQueryExecutorFromString(
                            executor, estimator, randomAccessCache,
                            sortedAccessCache);

                    boolean isTAExecutor = argmaxQueryExecutor instanceof ThresholdArgmaxQueryExecutor;
                    boolean isNRAExecutor = argmaxQueryExecutor instanceof NoRandomAccessArgmaxQueryExecutor;

                    BigInteger timeSum = BigInteger.ZERO;
                    int numCalcs = 0;

                    try {
                        openWriters(queryFile, phase, isTAExecutor,
                                isNRAExecutor);

                        for (int i = 0; i != times + 1; ++i) {
                            // Trigger garbage collection at begin of every benchmark
                            // iteration, to avoid triggering it mid benchmark.
                            System.gc();

                            try (BufferedReader reader = Files.newBufferedReader(
                                    queryFile, Constants.CHARSET)) {
                                boolean firstLine = true;
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    int lastSpacePos = line.lastIndexOf(' ');
                                    String history = line.substring(0,
                                            lastSpacePos);
                                    String sequence = line.substring(lastSpacePos + 1);

                                    for (int k = 0; k != limit; ++k) {
                                        long timeBefore = System.nanoTime();
                                        List<ArgmaxResult> argmaxResults = argmaxQueryExecutor.queryArgmax(
                                                history, k + 1);
                                        long timeAfter = System.nanoTime();

                                        long timeDelta = timeAfter - timeBefore;
                                        long timeKeystokes = timeDelta;

                                        if (i != 0) {
                                            // i == 0 is warmup run
                                            timeSum = timeSum.add(BigInteger.valueOf(timeDelta));
                                            ++numCalcs;

                                            if (resultsDir != null) {
                                                if (!firstLine) {
                                                    writersTime.get(k).append(
                                                            '\t');
                                                    if (isTAExecutor
                                                            || isNRAExecutor)
                                                        writersNumSortedAccesses.get(
                                                                k).append('\t');
                                                    if (isTAExecutor)
                                                        writersNumRandomAccesses.get(
                                                                k).append('\t');
                                                }
                                                writersTime.get(k).append(
                                                        Long.toString(timeDelta));
                                                if (isTAExecutor) {
                                                    ThresholdArgmaxQueryExecutor ta = (ThresholdArgmaxQueryExecutor) argmaxQueryExecutor;
                                                    writersNumSortedAccesses.get(
                                                            k).append(
                                                            Integer.toString(ta.getNumSortedAccesses()));
                                                    writersNumRandomAccesses.get(
                                                            k).append(
                                                            Integer.toString(ta.getNumRandomAccesses()));
                                                }
                                                if (isNRAExecutor) {
                                                    NoRandomAccessArgmaxQueryExecutor nra = (NoRandomAccessArgmaxQueryExecutor) argmaxQueryExecutor;
                                                    writersNumSortedAccesses.get(
                                                            k).append(
                                                            Integer.toString(nra.getNumSortedAccesses()));
                                                }
                                            }

                                            if (outputDir != null) {
                                                writersProbabilities.get(k).append(
                                                        format("%s : %s ",
                                                                history,
                                                                sequence));
                                                for (ArgmaxResult a : argmaxResults)
                                                    writersProbabilities.get(k).append(
                                                            format("[%s-%e]",
                                                                    a.getSequence(),
                                                                    a.getProbability()));
                                                writersProbabilities.get(k).append(
                                                        '\n');
                                            }
                                        }

                                        if (k == 0) {
                                            if (i != 0 && outputDir != null)
                                                writersProbabilities.get(k).append(
                                                        repeat("-", 80)).append(
                                                                '\n');

                                            int keystrokes = -1;
                                            ArgmaxResult argmaxResult = argmaxResults.get(0);
                                            if (argmaxResult.getSequence().equals(
                                                    sequence))
                                                keystrokes = 0;

                                            for (int prefixLength = 1; prefixLength != sequence.length() + 1; ++prefixLength) {
                                                String s = sequence.substring(
                                                        0, prefixLength);
                                                timeBefore = System.nanoTime();
                                                argmaxResults = argmaxQueryExecutor.queryArgmax(
                                                        history, s, k + 1);
                                                timeAfter = System.nanoTime();
                                                timeDelta = timeAfter
                                                        - timeBefore;

                                                // TODO: investigate why this happens for history="are said to be" sequence="underutilizied" on training-1 of oanc
                                                if (argmaxResults.size() != 0)
                                                    argmaxResult = argmaxResults.get(0);
                                                if (keystrokes == -1) {
                                                    timeKeystokes += timeDelta;
                                                    if (argmaxResult.getSequence().equals(
                                                            sequence))
                                                        keystrokes = prefixLength;
                                                }

                                                if (i != 0 && outputDir != null) {
                                                    writersProbabilities.get(k).append(
                                                            s).append(
                                                                    repeat("-",
                                                                            sequence.length()
                                                                            - prefixLength)).append(
                                                                                    " : ");
                                                    for (ArgmaxResult r : argmaxResults)
                                                        writersProbabilities.get(
                                                                k).append(
                                                                        format("[%s-%e]",
                                                                                r.getSequence(),
                                                                                r.getProbability()));
                                                    writersProbabilities.get(k).append(
                                                            '\n');
                                                }
                                            }

                                            if (i != 0 && outputDir != null)
                                                writersProbabilities.get(k).append(
                                                        '\n');

                                            if (keystrokes == -1)
                                                keystrokes = sequence.length();

                                            float keystokeSavings = (float) (sequence.length() - keystrokes)
                                                    / sequence.length();
                                            if (i != 0 && resultsDir != null) {
                                                if (!firstLine) {
                                                    writerKeystrokes.append('\t');
                                                    writerTimeKeystrokes.append('\t');
                                                }
                                                writerKeystrokes.append(Float.toString(keystokeSavings));
                                                writerTimeKeystrokes.append(Long.toString(timeKeystokes));
                                            }
                                        }
                                    }

                                    firstLine = false;
                                    progressBar.increase();
                                }

                                if (i != 0) {
                                    writerKeystrokes.append('\n');
                                    writerTimeKeystrokes.append('\n');
                                    for (int k = 0; k != limit; ++k) {
                                        if (resultsDir != null) {
                                            writersTime.get(k).append('\n');
                                            if (isTAExecutor || isNRAExecutor)
                                                writersNumSortedAccesses.get(k).append(
                                                        '\n');
                                            if (isTAExecutor)
                                                writersNumRandomAccesses.get(k).append(
                                                        '\n');
                                        }
                                        if (outputDir != null)
                                            if (k == 0)
                                                writersProbabilities.get(k).append(
                                                        repeat("=", 80)).append(
                                                                '\n');
                                            else
                                                writersProbabilities.get(k).append(
                                                        '\n');
                                    }
                                }
                            }
                        }
                    } finally {
                        closeWriters(isTAExecutor, isNRAExecutor);
                    }

                    BigInteger timePerArgmax = timeSum.divide(BigInteger.valueOf(numCalcs));
                    println("%s: %.2fms", phase,
                            timePerArgmax.floatValue() / 1000 / 1000);
                }
        }
    }

    private void openWriters(Path queryFile,
                             String phase,
                             boolean isTAExecutor,
                             boolean isNRAExecutor) throws IOException {
        if (resultsDir != null) {
            Path resultsFile = resultsDir.resolve(format("%s-%s", queryFile,
                    phase));

            writerKeystrokes = newBufferedWriter(Paths.get(format("%s-nkss",
                    resultsFile)), Constants.CHARSET);
            writerTimeKeystrokes = newBufferedWriter(Paths.get(format(
                    "%s-time-nkss", resultsFile)), Constants.CHARSET);
            writersTime = new ArrayList<>(limit);

            for (int k = 1; k != limit + 1; ++k)
                writersTime.add(newBufferedWriter(Paths.get(format("%s-%d",
                        resultsFile, k)), Constants.CHARSET));

            if (isTAExecutor || isNRAExecutor) {
                writersNumSortedAccesses = new ArrayList<>(limit);
                for (int k = 1; k != limit + 1; ++k)
                    writersNumSortedAccesses.add(newBufferedWriter(
                            Paths.get(format("%s-%d-numSortedAccesses",
                                    resultsFile, k)), Constants.CHARSET));
            }
            if (isTAExecutor) {
                writersNumRandomAccesses = new ArrayList<>(limit);
                for (int k = 1; k != limit + 1; ++k)
                    writersNumRandomAccesses.add(newBufferedWriter(
                            Paths.get(format("%s-%d-numRandomAccesses",
                                    resultsFile, k)), Constants.CHARSET));
            }
        }

        if (outputDir != null) {
            Path outputFile = outputDir.resolve(format("%s-%s", queryFile,
                    phase));

            writersProbabilities = new ArrayList<>(limit);
            for (int k = 1; k != limit + 1; ++k)
                writersProbabilities.add(newBufferedWriter(Paths.get(format(
                        "%s-%d", outputFile, k)), Constants.CHARSET));
        }
    }

    private void closeWriters(boolean isTAExecutor,
                              boolean isNRAExecutor) throws IOException {
        if (resultsDir != null) {
            writerKeystrokes.close();
            writerTimeKeystrokes.close();
            for (int k = 0; k != limit; ++k) {
                writersTime.get(k).close();
                if (isTAExecutor || isNRAExecutor)
                    writersNumSortedAccesses.get(k).close();
                if (isTAExecutor)
                    writersNumRandomAccesses.get(k).close();
            }
        }
        if (outputDir != null)
            for (int k = 0; k != limit; ++k)
                writersProbabilities.get(k).close();
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
