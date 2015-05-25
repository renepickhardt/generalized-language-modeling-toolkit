package de.glmtk.executables;

import static com.google.common.collect.Sets.newLinkedHashSet;
import static de.glmtk.common.Output.OUTPUT;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
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

    private Path corpus = null;
    private Path workingDir = null;
    private Set<Estimator> estimators = new LinkedHashSet<>();
    private Set<Path> queries = new LinkedHashSet<>();
    private Integer times = null;
    private Path cacheFile = null;

    @Override
    protected String getExecutableName() {
        return "glmtk-exp-estimatortime";
    }

    @Override
    protected void registerOptions() {
        optionCorpus = new CorpusOption("c", "corpus",
                "Give corpus and maybe working directory.");
        optionEstimators = new EstimatorsOption("e", "estimator",
                "Estimators to check.");
        optionQuery = new PathsOption("q", "query",
                "Query the given files. Can be specified multiple times.").requireMustExist().requireFiles();
        optionRuns = new IntegerOption("N", "num-runs",
                "Number of times to run. Default: 1.").defaultValue(1).requirePositive().requireNotZero();
        optionCacheFile = new PathOption("c", "cache-file",
                "File to generate query cache from for all query files.").requireMustExist().requireFile();

        optionManager.options(optionCorpus, optionEstimators, optionQuery,
                optionRuns, optionCacheFile);
    }

    @Override
    protected String getHelpHeader() {
        return "Splits the given corpus into training and test files.";
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
            throw new CliArgumentException(String.format(
                    "No estimators given, use %s.", optionEstimators));

        queries = newLinkedHashSet(optionQuery.getPaths());
        if (queries.isEmpty())
            throw new CliArgumentException(String.format(
                    "No files to query given, use %s.", optionQuery));

        times = optionRuns.getInt();
        cacheFile = optionCacheFile.getPath();
    }

    //
    //    private void parseFlags() throws IOException {
    //        @SuppressWarnings("unchecked")
    //        Iterator<Option> iter = line.iterator();
    //        while (iter.hasNext()) {
    //            Option option = iter.next();
    //
    //            if (option.equals(OPTION_ESTIMATOR))
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
    //            else if (option.equals(OPTION_QUERY))
    //                for (String opt : option.getValues())
    //                    queries.add(getAndCheckFile(opt));
    //
    //            else if (option.equals(OPTION_RUNS)) {
    //                optionFirstTimeOrFail(times, option);
    //                times = optionPositiveIntOrFail(option.getValue(), false,
    //                        "Illegal %s argument", makeOptionString(option));
    //
    //            } else if (option.equals(OPTION_CACHE_FILE)) {
    //                optionFirstTimeOrFail(cacheFile, option);
    //                cacheFile = getAndCheckFile(option.getValue());
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
        cacheSpec.withCounts(Patterns.getMany("x")); // FIXME: Refactor this!

        Set<Pattern> requiredPatterns = cacheSpec.getRequiredPatterns();
        requiredPatterns.add(Patterns.get("x1111x")); // FIXME: Refactor this!

        Cache cache = null;
        if (cacheFile != null) {
            GlmtkPaths queryCache = glmtk.provideQueryCache(cacheFile,
                    requiredPatterns);
            cache = cacheSpec.build(queryCache);
        }

        for (Path queryFile : queries) {
            OUTPUT.printMessage("");
            OUTPUT.printMessage(queryFile + ":");

            if (cacheFile == null) {
                GlmtkPaths queryCache = glmtk.provideQueryCache(queryFile,
                        requiredPatterns);
                cache = cacheSpec.build(queryCache);
            }

            for (Estimator estimator : estimators) {
                estimator.setCache(cache);

                BigInteger timeSum = BigInteger.ZERO;
                int n = 0;

                for (int i = 0; i != times; ++i)
                    try (BufferedReader reader = Files.newBufferedReader(
                            queryFile, Constants.CHARSET)) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            List<String> words = StringUtils.split(line, ' ');
                            NGram sequence = new NGram(
                                    words.get(words.size() - 1));
                            NGram history = new NGram(words.subList(0,
                                    words.size() - 1));

                            long timeBefore = System.nanoTime();

                            double prob = estimator.probability(sequence,
                                    history);

                            long timeAfter = System.nanoTime();

                            LOGGER.trace("P(%s | %s) = %e", sequence, history,
                                    prob);

                            timeSum = timeSum.add(BigInteger.valueOf(timeAfter
                                    - timeBefore));
                            ++n;
                        }
                    }

                BigInteger timePerProbability = timeSum.divide(BigInteger.valueOf(n));
                OUTPUT.printMessage(String.format("%s: %sns",
                        estimator.getName(), timePerProbability));
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
        LOGGER.debug("Estimators: %s", estimators);
        LOGGER.debug("Queries:    %s", queries);
    }
}
