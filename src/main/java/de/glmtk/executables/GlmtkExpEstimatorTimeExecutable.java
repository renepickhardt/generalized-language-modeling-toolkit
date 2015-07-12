package de.glmtk.executables;

import static com.google.common.collect.Sets.newLinkedHashSet;
import static de.glmtk.output.Output.println;
import static de.glmtk.util.NioUtils.countNumberOfLines;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.newBufferedReader;
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
import de.glmtk.common.NGram;
import de.glmtk.common.Pattern;
import de.glmtk.common.Patterns;
import de.glmtk.exceptions.CliArgumentException;
import de.glmtk.logging.Logger;
import de.glmtk.options.IntegerOption;
import de.glmtk.options.PathOption;
import de.glmtk.options.PathsOption;
import de.glmtk.options.custom.CorpusOption;
import de.glmtk.options.custom.EstimatorsOption;
import de.glmtk.output.ProgressBar;
import de.glmtk.querying.estimator.Estimator;
import de.glmtk.querying.estimator.weightedsum.WeightedSumEstimator;
import de.glmtk.querying.estimator.weightedsum.WeightedSumFunction;
import de.glmtk.util.StringUtils;

public class GlmtkExpEstimatorTimeExecutable extends Executable {
    private static final Logger LOGGER = Logger.get(GlmtkExpEstimatorTimeExecutable.class);

    public static void main(String[] args) {
        new GlmtkExpEstimatorTimeExecutable().run(args);
    }

    private CorpusOption optionCorpus;
    private EstimatorsOption optionEstimators;
    private PathsOption optionQuery;
    private IntegerOption optionRuns;
    private PathOption optionCacheFile;
    private PathOption optionResultsDir;

    private Path corpus;
    private Path workingDir;
    private Set<Estimator> estimators;
    private Set<Path> queries;
    private Integer times;
    private Path cacheFile;
    private Path resultsDir;
    private ProgressBar progressBar;

    @Override
    protected String getExecutableName() {
        return "glmtk-exp-estimatortime";
    }

    @Override
    protected void registerOptions() {
        optionCorpus = new CorpusOption(null, "corpus",
                "Give corpus and maybe working directory.");
        optionEstimators = new EstimatorsOption("e", "estimator",
                "Estimators to check.");
        optionQuery = new PathsOption("q", "query",
                "Query the given files. Can be specified multiple times.").requireMustExist().requireFiles();
        optionRuns = new IntegerOption("N", "num-runs",
                "Number of times to run. Default: 1.").defaultValue(1).requirePositive().requireNotZero();
        optionCacheFile = new PathOption("c", "cache-file",
                "File to generate query cache from for all query files.").requireMustExist().requireFile();
        optionResultsDir = new PathOption("d", "results-dir",
                "Directory to store all results like times or numWeights.").requireMayExist().requireDirectory();

        commandLine.inputArgs(optionCorpus);
        commandLine.options(optionEstimators, optionQuery, optionRuns,
                optionCacheFile, optionResultsDir);
    }

    @Override
    protected String getHelpHeader() {
        return "Performs estimator time experiment.";
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

        estimators = newLinkedHashSet(optionEstimators.getEstimators());
        if (estimators.isEmpty())
            throw new CliArgumentException("No estimators given, use %s.",
                    optionEstimators);

        queries = newLinkedHashSet(optionQuery.getPaths());
        if (queries.isEmpty())
            throw new CliArgumentException("No files to query given, use %s.",
                    optionQuery);

        times = optionRuns.getInt();
        cacheFile = optionCacheFile.getPath();
        resultsDir = optionResultsDir.getPath();
    }

    @Override
    protected void exec() throws Exception {
        logFields();

        List<String> phases = new ArrayList<>(estimators.size());
        for (Estimator estimator : estimators)
            phases.add(estimator.getName());
        progressBar = new ProgressBar(phases);

        Glmtk glmtk = new Glmtk(config, corpus, workingDir);

        int neededOrder = getNeededOrder();

        CacheSpecification cacheSpec = new CacheSpecification();
        cacheSpec.withProgress();
        for (Estimator estimator : estimators)
            cacheSpec.addAll(estimator.getRequiredCache(neededOrder));
        cacheSpec.withCounts(Patterns.getMany("x")); // FIXME: Refactor this!

        Set<Pattern> requiredPatterns = cacheSpec.getRequiredPatterns();
        requiredPatterns.add(Patterns.get("x1111x")); // FIXME: Refactor this!

        Cache cache = null;
        if (cacheFile != null) {
            GlmtkPaths queryCache = glmtk.provideQueryCache(cacheFile,
                    requiredPatterns);
            cache = cacheSpec.build(queryCache);
        }

        if (resultsDir != null)
            createDirectories(resultsDir);

        for (Path queryFile : queries) {
            println();
            println(queryFile + ":");

            if (cacheFile == null) {
                GlmtkPaths queryCache = glmtk.provideQueryCache(queryFile,
                        requiredPatterns);
                cache = cacheSpec.build(queryCache);
            }

            Iterator<String> phaseIter = phases.iterator();
            for (Estimator estimator : estimators) {
                int numLines = countNumberOfLines(queryFile);
                progressBar.setPhase(phaseIter.next(), numLines * (times + 1));

                boolean weightedSumEstimator = estimator instanceof WeightedSumEstimator;

                estimator.setCache(cache);

                BigInteger timeSum = BigInteger.ZERO;
                int numProbs = 0;

                BufferedWriter writerTime = null;
                BufferedWriter writerTimeWeights = null;
                BufferedWriter writerTimeRemaining = null;
                BufferedWriter writerNumWeights = null;
                try {
                    if (resultsDir != null) {
                        Path resultsFile = resultsDir.resolve(queryFile + "-"
                                + estimator);
                        writerTime = newBufferedWriter(resultsFile,
                                Constants.CHARSET);
                        if (weightedSumEstimator) {
                            writerTimeWeights = newBufferedWriter(
                                    Paths.get(resultsFile + "-timeWeights"),
                                    Constants.CHARSET);
                            writerTimeRemaining = newBufferedWriter(
                                    Paths.get(resultsFile + "-timeRemaining"),
                                    Constants.CHARSET);
                            writerNumWeights = newBufferedWriter(
                                    Paths.get(resultsFile + "-numWeights"),
                                    Constants.CHARSET);
                        }
                    }

                    for (int i = 0; i != times + 1; ++i) {
                        // Trigger garbage collection at begin of every benchmark
                        // iteration, to avoid triggering it mid benchmark.
                        System.gc();

                        try (BufferedReader reader = Files.newBufferedReader(
                                queryFile, Constants.CHARSET)) {
                            boolean firstLine = true;
                            String line;
                            while ((line = reader.readLine()) != null) {
                                List<String> words = StringUtils.split(line,
                                        ' ');
                                NGram sequence = new NGram(
                                        words.get(words.size() - 1));
                                NGram history = new NGram(words.subList(0,
                                        words.size() - 1));

                                long timeDelta = 0, timeDeltaWeights = 0, timeDeltaRemaining = 0;
                                int numWeights = 0;
                                double prob;

                                if (!weightedSumEstimator) {
                                    long timeBefore = System.nanoTime();
                                    prob = estimator.probability(sequence,
                                            history);
                                    long timeAfter = System.nanoTime();

                                    timeDelta = timeAfter - timeBefore;
                                } else {
                                    long timeBeforeWeights = System.nanoTime();
                                    WeightedSumFunction weightedSumFunction = ((WeightedSumEstimator) estimator).calcWeightedSumFunction(history);
                                    long timeAfterWeights = System.nanoTime();
                                    numWeights = weightedSumFunction.size();

                                    long timeBeforeRemaining = System.nanoTime();
                                    prob = ((WeightedSumEstimator) estimator).probability(
                                            sequence, weightedSumFunction);
                                    long timeAfterRemaining = System.nanoTime();

                                    timeDeltaWeights = timeAfterWeights
                                            - timeBeforeWeights;
                                    timeDeltaRemaining = timeAfterRemaining
                                            - timeBeforeRemaining;
                                    timeDelta = timeDeltaWeights
                                            + timeDeltaRemaining;
                                }

                                LOGGER.trace("P(%s | %s) = %e", sequence,
                                        history, prob);

                                if (i != 0) {
                                    // i == 0 is warmup run

                                    timeSum = timeSum.add(BigInteger.valueOf(timeDelta));
                                    ++numProbs;

                                    if (resultsDir != null) {
                                        if (firstLine)
                                            firstLine = false;
                                        else {
                                            writerTime.append('\t');
                                            if (weightedSumEstimator) {
                                                writerTimeWeights.append('\t');
                                                writerTimeRemaining.append('\t');
                                                writerNumWeights.append('\t');
                                            }
                                        }
                                        writerTime.append(Long.toString(timeDelta));
                                        if (weightedSumEstimator) {
                                            writerTimeWeights.append(Long.toString(timeDeltaWeights));
                                            writerTimeRemaining.append(Long.toString(timeDeltaRemaining));
                                            writerNumWeights.append(Integer.toString(numWeights));
                                        }
                                    }
                                }

                                progressBar.increase(1);
                            }
                        }

                        if (i != 0 && resultsDir != null) {
                            writerTime.append('\n');
                            if (weightedSumEstimator) {
                                writerTimeWeights.append('\n');
                                writerTimeRemaining.append('\n');
                                writerNumWeights.append('\n');
                            }
                        }
                    }

                } finally {
                    if (resultsDir != null) {
                        writerTime.close();
                        if (weightedSumEstimator) {
                            writerTimeWeights.close();
                            writerTimeRemaining.close();
                            writerNumWeights.close();
                        }
                    }
                }

                BigInteger timePerProbability = timeSum.divide(BigInteger.valueOf(numProbs));
                println("%s: %ssns", estimator.getName(), timePerProbability);
            }
        }
    }

    private int getNeededOrder() throws IOException {
        int neededOrder = 0;
        for (Path queryFile : queries)
            try (BufferedReader reader = newBufferedReader(queryFile,
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
        LOGGER.debug("Estimators: %s", estimators);
        LOGGER.debug("Queries:    %s", queries);
        LOGGER.debug("CacheFile:  %s", cacheFile);
        LOGGER.debug("OutputDir:  %s", resultsDir);
    }
}
