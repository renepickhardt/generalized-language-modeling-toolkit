package de.glmtk.executables;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.Option;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.Constants;
import de.glmtk.Status;
import de.glmtk.Status.TrainingStatus;
import de.glmtk.counting.AbsoluteCounter;
import de.glmtk.counting.ContinuationCounter;
import de.glmtk.counting.Tagger;
import de.glmtk.pattern.Pattern;
import de.glmtk.pattern.PatternElem;
import de.glmtk.smoothing.Corpus;
import de.glmtk.smoothing.NGramProbabilityCalculator;
import de.glmtk.smoothing.ProbMode;
import de.glmtk.smoothing.estimator.Estimator;
import de.glmtk.smoothing.estimator.Estimators;
import de.glmtk.utils.Logging;
import de.glmtk.utils.StatisticalNumberHelper;
import de.glmtk.utils.StringUtils;

public class GlmtkNew extends Executable {

    private static final Logger LOGGER = LogManager
            .getFormatterLogger(GlmtkNew.class);

    private static final String OPTION_OUTPUT = "output";

    private static final String OPTION_MODEL = "model";

    private static final String OPTION_TESTING = "testing";

    private static List<Option> options;
    static {
        //@formatter:off
        Option help    = new Option("h", OPTION_HELP,    false, "Print this message.");
        Option version = new Option("v", OPTION_VERSION, false, "Print the version information and exit.");
        Option output  = new Option("o", OPTION_OUTPUT,  true,  "Use given directory for output.");
        output.setArgName("OUTPUTDIR");
        Option model   = new Option("m", OPTION_MODEL,   true,  "KN  - Kneser Ney\n" +
                "MKN - Modified Kneser Ney\n" +
                "GLM - Generalized Language Model");
        model.setArgName("MODEL");
        Option testing = new Option("t", OPTION_TESTING, true,  "File to take testing sequences for probability and entropy from (can be specified multiple times).");
        testing.setArgName("TESTING");
        //@formatter:on
        options = Arrays.asList(help, version, output, model, testing);
    }

    private Model model = null;

    private Path corpus = null;

    private Path output = null;

    private List<Path> testing = new LinkedList<Path>();

    public static void main(String[] args) {
        new GlmtkNew().run(args);
    }

    @Override
    protected List<Option> getOptions() {
        return options;
    }

    @Override
    protected String getUsage() {
        return "glmtk-new [OPTION]... <CORPUS>";
    }

    @Override
    protected void configureLogging() {
        super.configureLogging();
        Logging.addLocalFileAppender(output.resolve("log"));
    }

    @Override
    protected void parseArguments(String[] args) {
        super.parseArguments(args);

        if (line.getArgs() == null || line.getArgs().length == 0) {
            System.err.println("Missing corpus\n"
                    + "Try 'glmtk-new --help' for more information.");
            throw new Termination();
        }

        if (line.hasOption(OPTION_MODEL)) {
            switch (line.getOptionValue(OPTION_MODEL).toUpperCase()) {
                case "KN":
                    model = Model.KNESER_NEY;
                    break;
                case "MKN":
                    model = Model.MODIFIED_KNESER_NEY;
                    break;
                case "GLM":
                    model = Model.GENERALIZED_LANGUAGE_MODEL;
                    break;

                default:
                    System.err.println("Unkown model option '"
                            + line.getOptionValue(OPTION_MODEL) + "'.");
                    throw new Termination();
            }
        } else {
            model = Model.MODIFIED_KNESER_NEY;
        }

        corpus = Paths.get(line.getArgs()[0]);
        if (!(Files.exists(corpus) && Files.isReadable(corpus))) {
            System.err.println("Corpus '" + corpus
                    + "' does not exist or is not readable.");
            throw new Termination();
        }
        if (Files.isDirectory(corpus)) {
            Path statusFile = corpus.resolve("status");
            Path trainingFile = corpus.resolve("training");
            if (!(Files.exists(statusFile) && Files.isReadable(statusFile))) {
                System.err.println("Corpus status file '" + statusFile
                        + "' does not exist or is not readable.");
                throw new Termination();
            }
            if (!(Files.exists(trainingFile) && Files.isReadable(trainingFile))) {
                System.err.println("Corpus training file '" + trainingFile
                        + "' does not exist or is not readable.");
                throw new Termination();
            }
            if (line.hasOption(OPTION_OUTPUT)) {
                System.err
                .println("Can't specify output directory if using existing corpus as input.");
                throw new Termination();
            }
            output = corpus;
            corpus = trainingFile;
        } else {
            if (line.hasOption(OPTION_OUTPUT)) {
                output = Paths.get(line.getOptionValue(OPTION_OUTPUT));
            } else {
                output = Paths.get(corpus + ".out");
            }
            if (Files.exists(output) && !Files.isDirectory(output)) {
                System.err.println("Output file '" + output
                        + "' already exists but is not a directory.");
            }
        }

        if (line.hasOption(OPTION_TESTING)) {
            for (String testingFile : line.getOptionValues(OPTION_TESTING)) {
                testing.add(Paths.get(testingFile.trim()));
            }
        }
    }

    @Override
    protected void exec() throws IOException {
        if (!Files.exists(output)) {
            Files.createDirectories(output);
        }

        Path statusFile = output.resolve("status");
        Path trainingFile = output.resolve("training");
        Path absoluteDir = output.resolve("absolute");
        Path absoluteTmpDir = output.resolve("absolute.tmp");
        Path continuationDir = output.resolve("continuation");
        Path continuationTmpDir = output.resolve("continuation.tmp");
        Path testingDir = output.resolve("testing");

        Status status = new Status(statusFile, corpus);
        status.logStatus();

        // TODO: check file system if status is accurate.
        // TODO: update status with smaller increments (each completed pattern).

        // Request /////////////////////////////////////////////////////////////

        // Whether the corpus should be tagged with POS.
        boolean needToTagTraining = false;
        // Absolute Patterns we need
        Set<Pattern> neededAbsolutePatterns = null;
        //                Pattern.getCombinations(5, Arrays.asList(PatternElem.CNT,
        //                        PatternElem.SKP, PatternElem.POS));
        // Continuation Patterns we need
        Set<Pattern> neededContinuationPatterns = null;
        //                Pattern.replaceTargetWithElems(neededAbsolutePatterns,
        //                        PatternElem.SKP,
        //                        Arrays.asList(PatternElem.WSKP, PatternElem.PSKP));

        // TODO: optimize to only count needed patterns for KN and MKN.
        switch (model) {
            case KNESER_NEY:
            case MODIFIED_KNESER_NEY:
            case GENERALIZED_LANGUAGE_MODEL:
                neededAbsolutePatterns =
                Pattern.getCombinations(5,
                        Arrays.asList(PatternElem.CNT, PatternElem.SKP));
                neededContinuationPatterns =
                        Pattern.replaceTargetWithElems(neededAbsolutePatterns,
                                PatternElem.SKP,
                                Arrays.asList(PatternElem.WSKP));
                break;
            default:
                throw new IllegalStateException();
        }

        // Add patterns to absolute that are needed to generate continuation.
        for (Pattern pattern : neededContinuationPatterns) {
            Pattern sourcePattern = pattern.getContinuationSource();
            if (sourcePattern.isAbsolute()) {
                neededAbsolutePatterns.add(sourcePattern);
            }
        }

        LOGGER.debug("Request %s", StringUtils.repeat("-", 80 - 8));
        LOGGER.debug("needToTagTraning           = %s", needToTagTraining);
        LOGGER.debug("neededAbsolutePatterns     = %s", neededAbsolutePatterns);
        LOGGER.debug("neededContinuationPatterns = %s",
                neededContinuationPatterns);

        // Training / Tagging //////////////////////////////////////////////////

        // TODO: doesn't detect the setting that user changed from untagged
        // training file, to tagged file with same corpus.
        // TODO: doesn't detect when switching from untagged training to
        // continuing with now tagged corpus.
        if (needToTagTraining) {
            if (status.getTraining() == TrainingStatus.DONE_WITH_POS) {
                LOGGER.info("Detected tagged training already present, skipping tagging.");
            } else {
                Files.deleteIfExists(trainingFile);
                Tagger tagger =
                        new Tagger(config.getUpdateInterval(),
                                config.getModel());
                tagger.tag(corpus, trainingFile);
                status.setTraining(TrainingStatus.DONE_WITH_POS, trainingFile);
            }
        } else {
            if (status.getTraining() != TrainingStatus.NONE) {
                LOGGER.info("Detected training already present, skipping copying training.");
            } else {
                Files.deleteIfExists(trainingFile);
                Files.copy(corpus, trainingFile);
                status.setTraining(TrainingStatus.DONE, trainingFile);
            }
        }

        // Absolute ////////////////////////////////////////////////////////////

        AbsoluteCounter absoluteCounter =
                new AbsoluteCounter(neededAbsolutePatterns,
                        config.getNumberOfCores(), config.getUpdateInterval());
        absoluteCounter
        .count(status, trainingFile, absoluteDir, absoluteTmpDir);

        // Continuation ////////////////////////////////////////////////////////

        ContinuationCounter continuationCounter =
                new ContinuationCounter(neededContinuationPatterns,
                        config.getNumberOfCores(), config.getUpdateInterval());
        continuationCounter.count(status, absoluteDir, absoluteTmpDir,
                continuationDir, continuationTmpDir);

        // Testing /////////////////////////////////////////////////////////////

        if (!testing.isEmpty()) {
            testing(absoluteDir, continuationDir, testingDir);
        }

        // Used for debugging. Will only print output if averalmtges are added
        // somewhere else in the code.
        StatisticalNumberHelper.print();
    }

    private void
        testing(Path absoluteDir, Path continuationDir, Path testingDir)
                throws IOException {
        Files.createDirectories(testingDir);

        LOGGER.info("Loading counts into memory...");
        Corpus corpus = new Corpus(absoluteDir, continuationDir);

        Estimator estimator = null;
        switch (model) {
            case MODIFIED_KNESER_NEY:
                estimator = Estimators.INTERPOL_ABS_DISCOUNT_MLE_REC;
                //estimator = Estimators.MODIFIED_KNESER_NEY_ESIMATOR;
                break;
            default:
                throw new UnsupportedOperationException();
        }
        estimator.setCorpus(corpus);

        NGramProbabilityCalculator calculator =
                new NGramProbabilityCalculator();
        calculator.setProbMode(ProbMode.MARG);
        calculator.setEstimator(estimator);

        for (Path testingFile : testing) {
            SimpleDateFormat format =
                    new SimpleDateFormat(" yyyy-MM-dd HH:mm:ss");
            Path outputFile =
                    testingDir.resolve(testingFile.getFileName()
                            + format.format(new Date()));
            Files.deleteIfExists(outputFile);

            LOGGER.info("Testing File: '%s' -> '%s'.", testingFile, outputFile);

            try (BufferedReader reader =
                    Files.newBufferedReader(testingFile,
                            Charset.defaultCharset());
                    BufferedWriter writer =
                            Files.newBufferedWriter(outputFile,
                                    Charset.defaultCharset())) {
                int cntZero = 0;
                int cntNonZero = 0;
                double sumProbabilities = 0;
                double entropy = 0;
                double logBase = Math.log(Constants.LOG_BASE);

                String line;
                while ((line = reader.readLine()) != null) {
                    double probability =
                            calculator.probability(StringUtils.splitAtChar(
                                    line, ' '));

                    if (probability == 0) {
                        ++cntZero;
                    } else {
                        ++cntNonZero;
                        sumProbabilities += probability;
                        entropy -= Math.log(probability) / logBase;
                    }

                    writer.append(line);
                    writer.append('\t');
                    writer.append(((Double) probability).toString());
                    writer.append('\n');
                }

                LOGGER.info("Count Zero-Propablity Sequences = %s (%6.2f%%)",
                        cntZero, (double) cntZero / (cntZero + cntNonZero)
                        * 100);
                LOGGER.info(
                        "Count Non-Zero-Propability Sequences = %s (%6.2f%%)",
                        cntNonZero, (double) cntNonZero
                        / (cntZero + cntNonZero) * 100);
                LOGGER.info("Sum of Propabilities = %s", sumProbabilities);
                LOGGER.info("Entropy = %s", entropy);

            }
        }
    }
}
