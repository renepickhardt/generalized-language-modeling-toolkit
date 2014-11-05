package de.glmtk;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.Status.TrainingStatus;
import de.glmtk.counting.AbsoluteCounter;
import de.glmtk.counting.ContinuationCounter;
import de.glmtk.counting.Tagger;
import de.glmtk.pattern.Pattern;
import de.glmtk.pattern.PatternElem;
import de.glmtk.smoothing.CountCache;
import de.glmtk.smoothing.NGramProbabilityCalculator;
import de.glmtk.smoothing.ProbMode;
import de.glmtk.smoothing.estimator.Estimator;
import de.glmtk.smoothing.estimator.Estimators;
import de.glmtk.utils.StringUtils;

public class Glmtk {

    private static final Logger LOGGER = LogManager
            .getFormatterLogger(Glmtk.class);

    private Model model = Model.MODIFIED_KNESER_NEY;

    private Config config = Config.get();

    private Path corpus = null;

    private Path workingDir = null;

    private Path statusFile = null;

    private Path trainingFile = null;

    private Path absoluteDir = null;

    private Path absoluteTmpDir = null;

    private Path continuationDir = null;

    private Path continuationTmpDir = null;

    private Path testingDir = null;

    private List<Path> testingFiles = new LinkedList<Path>();

    public void setModel(Model model) {
        this.model = model;
    }

    public void setCorpus(Path corpus) {
        this.corpus = corpus;
    }

    public void setWorkingDir(Path workingDir) {
        this.workingDir = workingDir;
        statusFile = workingDir.resolve("status");
        trainingFile = workingDir.resolve("training");
        absoluteDir = workingDir.resolve("absolute");
        absoluteTmpDir = workingDir.resolve("absolute.tmp");
        continuationDir = workingDir.resolve("continuation");
        continuationTmpDir = workingDir.resolve("continuation.tmp");
        testingDir = workingDir.resolve("testing");
    }

    public void addTestingFile(Path testingFile) {
        testingFiles.add(testingFile);
    }

    public void count() throws IOException {
        if (!Files.exists(workingDir)) {
            Files.createDirectories(workingDir);
        }

        Status status = new Status(statusFile, corpus);
        status.logStatus();

        // TODO: check file system if status is accurate.
        // TODO: update status with smaller increments (each completed pattern).

        // Request /////////////////////////////////////////////////////////////

        // Whether the corpus should be tagged with POS.
        boolean needToTagTraining = false;
        // Absolute Patterns we need
        Set<Pattern> neededAbsolutePatterns = null;
        // Continuation Patterns we need
        Set<Pattern> neededContinuationPatterns = null;

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
                //                neededAbsolutePatterns =
                //                        Pattern.getCombinations(5, Arrays.asList(
                //                                PatternElem.CNT, PatternElem.SKP,
                //                                PatternElem.POS));
                //                neededContinuationPatterns =
                //                        Pattern.replaceTargetWithElems(neededAbsolutePatterns,
                //                                PatternElem.SKP, Arrays.asList(
                //                                        PatternElem.WSKP, PatternElem.PSKP));
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
                // TODO: check if this breaks if corpus = trainingFile
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
                // TODO: check if this breaks if corpus = trainingFile
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
    }

    public void test() throws IOException {
        if (testingFiles.isEmpty()) {
            return;
        }

        Files.createDirectories(testingDir);

        LOGGER.info("Loading counts into memory...");
        CountCache countCache = new CountCache(workingDir);

        Estimator estimator = getEstimatorFromModel(model);
        estimator.setCountCache(countCache);

        NGramProbabilityCalculator calculator =
                new NGramProbabilityCalculator();
        calculator.setProbMode(ProbMode.MARG);
        calculator.setEstimator(estimator);

        for (Path testingFile : testingFiles) {
            test(testingFile);
        }
    }

    private static Estimator getEstimatorFromModel(Model model) {
        switch (model) {
            case MODIFIED_KNESER_NEY:
                return Estimators.MODIFIED_KNESER_NEY_ESIMATOR;

            default:
                throw new UnsupportedOperationException();
        }
    }

    private void test(Path testingFile) throws IOException {
        SimpleDateFormat dateFormat =
                new SimpleDateFormat(" yyyy-MM-dd HH:mm:ss");
        Path outputFile =
                testingDir.resolve(testingFile.getFileName()
                        + dateFormat.format(new Date()));
        Files.deleteIfExists(outputFile);

        try (BufferedReader reader =
                Files.newBufferedReader(testingFile, Charset.defaultCharset());
                BufferedWriter writer =
                        Files.newBufferedWriter(outputFile,
                                Charset.defaultCharset())) {
        }
    }

}
