package de.glmtk.executables;

import static com.google.common.collect.Sets.newLinkedHashSet;
import static de.glmtk.output.Output.println;
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
import java.util.LinkedHashSet;
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
    private PathOption optionOutputDir;

    private Path corpus = null;
    private Path workingDir = null;
    private Set<Estimator> estimators = new LinkedHashSet<>();
    private Set<Path> queries = new LinkedHashSet<>();
    private Integer times = null;
    private Path cacheFile = null;
    private Path outputDir = null;

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
        optionOutputDir = new PathOption("o", "output-dir",
                "Output directory to store all results").requireMayExist().requireDirectory();

        commandLine.inputArgs(optionCorpus);
        commandLine.options(optionEstimators, optionQuery, optionRuns,
                optionCacheFile, optionOutputDir);
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
        outputDir = optionOutputDir.getPath();
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
        cacheSpec.withCounts(Patterns.getMany("x")); // FIXME: Refactor this!

        Set<Pattern> requiredPatterns = cacheSpec.getRequiredPatterns();
        requiredPatterns.add(Patterns.get("x1111x")); // FIXME: Refactor this!

        Cache cache = null;
        if (cacheFile != null) {
            GlmtkPaths queryCache = glmtk.provideQueryCache(cacheFile,
                    requiredPatterns);
            cache = cacheSpec.build(queryCache);
        }

        if (outputDir != null)
            createDirectories(outputDir);

        for (Path queryFile : queries) {
            println();
            println(queryFile + ":");

            if (cacheFile == null) {
                GlmtkPaths queryCache = glmtk.provideQueryCache(queryFile,
                        requiredPatterns);
                cache = cacheSpec.build(queryCache);
            }

            for (Estimator estimator : estimators) {
                boolean weightedSumEstimator = estimator instanceof WeightedSumEstimator;

                estimator.setCache(cache);

                BigInteger timeSum = BigInteger.ZERO;
                int numProbs = 0;

                BufferedWriter writer = null, writerNumWeights = null, writerTimeWeights = null, writerTimeRemaining = null;
                if (outputDir != null) {
                    Path outputFile = outputDir.resolve(queryFile + "-"
                            + estimator);
                    writer = newBufferedWriter(outputFile, Constants.CHARSET);
                    if (weightedSumEstimator) {
                        writerNumWeights = newBufferedWriter(
                                Paths.get(outputFile + "-numWeights"),
                                Constants.CHARSET);
                        writerTimeWeights = newBufferedWriter(
                                Paths.get(outputFile + "-timeWeights"),
                                Constants.CHARSET);
                        writerTimeRemaining = newBufferedWriter(
                                Paths.get(outputFile + "-timeRemaining"),
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
                            List<String> words = StringUtils.split(line, ' ');
                            NGram sequence = new NGram(
                                    words.get(words.size() - 1));
                            NGram history = new NGram(words.subList(0,
                                    words.size() - 1));

                            long timeDelta = 0, timeDeltaWeights = 0, timeDeltaRemaining = 0;
                            int numWeights = 0;
                            double prob;

                            if (!weightedSumEstimator) {
                                long timeBefore = System.nanoTime();
                                prob = estimator.probability(sequence, history);
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

                            LOGGER.trace("P(%s | %s) = %e", sequence, history,
                                    prob);

                            if (i != 0) {
                                // i == 0 is warmup run

                                timeSum = timeSum.add(BigInteger.valueOf(timeDelta));
                                ++numProbs;

                                if (outputDir != null) {
                                    if (firstLine)
                                        firstLine = false;
                                    else {
                                        writer.append('\t');
                                        if (weightedSumEstimator) {
                                            writerNumWeights.append('\t');
                                            writerTimeWeights.append('\t');
                                            writerTimeRemaining.append('\t');
                                        }
                                    }
                                    writer.append(Long.toString(timeDelta));
                                    if (weightedSumEstimator) {
                                        writerNumWeights.append(Integer.toString(numWeights));
                                        writerTimeWeights.append(Long.toString(timeDeltaWeights));
                                        writerTimeRemaining.append(Long.toString(timeDeltaRemaining));
                                    }
                                }
                            }
                        }
                    }

                    if (i != 0 && outputDir != null) {
                        writer.append('\n');
                        if (weightedSumEstimator) {
                            writerNumWeights.append('\n');
                            writerTimeWeights.append('\n');
                            writerTimeRemaining.append('\n');
                        }
                    }
                }

                if (outputDir != null) {
                    writer.close();
                    if (weightedSumEstimator) {
                        writerNumWeights.close();
                        writerTimeWeights.close();
                        writerTimeRemaining.close();
                    }
                }

                BigInteger timePerProbability = timeSum.divide(BigInteger.valueOf(numProbs));
                println("%s: %sns", estimator.getName(), timePerProbability);
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
        LOGGER.debug("OutputDir:  %s", outputDir);
    }
}
