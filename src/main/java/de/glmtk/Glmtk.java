package de.glmtk;

import static de.glmtk.utils.PatternElem.CNT;
import static de.glmtk.utils.PatternElem.POS;
import static de.glmtk.utils.PatternElem.PSKP;
import static de.glmtk.utils.PatternElem.SKP;
import static de.glmtk.utils.PatternElem.WSKP;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.Status.TrainingStatus;
import de.glmtk.learning.AbsoluteCounter;
import de.glmtk.learning.ContinuationCounter;
import de.glmtk.learning.Tagger;
import de.glmtk.querying.CountCache;
import de.glmtk.querying.NGramProbabilityCalculator;
import de.glmtk.querying.ProbMode;
import de.glmtk.querying.estimator.Estimator;
import de.glmtk.utils.Pattern;
import de.glmtk.utils.StringUtils;

/**
 * Here happens counting (during training phase) and also querying (during
 * application phase) This class is been called either by one of the Executable
 * classes and filled from config or console input or it is called from
 * UnitTests
 * Expects parameters to be set via setters before calling any other method
 * TODO: what about default parameters
 *
 */
public class Glmtk {

    // TODO: Output should be empty if a phase is skipped
    // TODO: Some Unicode bug prevents "海底軍艦 , to be Undersea" from turning
    // up in en0008t corpus absolute 11111 counts.
    // TODO: Detect ngram model length from testing.

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

        boolean needPos = false;

        // Whether the corpus should be tagged with POS.
        boolean needToTagTraining = false;
        // Absolute Patterns we need
        Set<Pattern> neededAbsolutePatterns = null;
        // Continuation Patterns we need
        Set<Pattern> neededContinuationPatterns = null;

        // TODO: optimize to only count needed patterns for KN and MKN.
        switch (model) {
            case MAXIMUM_LIKELIHOOD:
            case KNESER_NEY:
            case MODIFIED_KNESER_NEY:
            case GENERALIZED_LANGUAGE_MODEL:
                neededAbsolutePatterns =
                Pattern.getCombinations(Constants.MODEL_SIZE, needPos
                        ? Arrays.asList(CNT, SKP, POS)
                                : Arrays.asList(CNT, SKP));
                neededContinuationPatterns = new HashSet<Pattern>();
                for (Pattern pattern : neededAbsolutePatterns) {
                    if (pattern.contains(SKP)) {
                        neededContinuationPatterns.add(pattern.replace(SKP,
                                WSKP));
                        if (needPos) {
                            neededContinuationPatterns.add(pattern.replace(SKP,
                                    PSKP));
                        }
                    }
                }

                for (Pattern pattern : neededAbsolutePatterns) {
                    if (pattern.size() != Constants.MODEL_SIZE) {
                        neededContinuationPatterns.add(pattern.concat(WSKP));
                    }
                }
                break;
            default:
                throw new IllegalStateException();
        }

        // Add patterns to absolute that are needed to generate continuation.
        for (Pattern pattern : neededContinuationPatterns) {
            //            Pattern sourcePattern =
            //                    pattern.range(0, pattern.size() - 1).concat(CNT);
            //            neededAbsolutePatterns.add(sourcePattern);
            Pattern sourcePattern = pattern.getContinuationSource();
            if (sourcePattern.isAbsolute()) {
                neededAbsolutePatterns.add(sourcePattern);
            } else {
                neededContinuationPatterns.add(sourcePattern);
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
        //TODO: make a persistent version of what is needed for testing. also calculate hashvalues of it...
        CountCache countCache = new CountCache(workingDir);

        LOGGER.info("Loading model '%s'...", model.getName());
        Estimator estimator = model.getEstimator();
        estimator.setCountCache(countCache);

        NGramProbabilityCalculator calculator =
                new NGramProbabilityCalculator();
        calculator.setProbMode(ProbMode.MARG);
        calculator.setEstimator(estimator);

        for (Path testingFile : testingFiles) {
            test(calculator, testingFile);
        }
    }

    private void test(NGramProbabilityCalculator calculator, Path testingFile)
            throws IOException {
        SimpleDateFormat dateFormat =
                new SimpleDateFormat(" yyyy-MM-dd HH:mm:ss");
        Path outputFile =
                testingDir.resolve(testingFile.getFileName() + " "
                        + model.getAbbreviation()
                        + dateFormat.format(new Date()));
        Files.deleteIfExists(outputFile);

        LOGGER.info("Testing '%s' -> '%s'.", testingFile, outputFile);

        try (BufferedReader reader =
                Files.newBufferedReader(testingFile, Charset.defaultCharset());
                BufferedWriter writer =
                        Files.newBufferedWriter(outputFile,
                                Charset.defaultCharset())) {
            int cntZero = 0;
            int cntNonZero = 0;
            double sumProbabilities = 0;
            double crossEntropy = 0;
            double entropy = 0;
            double logBase = Math.log(Constants.LOG_BASE);

            String line;
            while ((line = reader.readLine()) != null) {
                double probability =
                        calculator.probability(StringUtils.splitAtChar(line,
                                ' '));

                if (probability == 0) {
                    ++cntZero;
                } else {
                    ++cntNonZero;
                    sumProbabilities += probability;
                    crossEntropy -= Math.log(probability);
                    entropy -= Math.log(probability) * probability;
                }

                writer.append(line);
                writer.append('\t');
                writer.append(Double.toString(probability));
                writer.append('\n');
            }

            if (cntNonZero != 0) {
                crossEntropy /= (cntNonZero * logBase);
                entropy /= logBase;
            }

            LOGGER.info("Count Zero-Propablity Sequences = %s (%6.2f%%)",
                    cntZero, (double) cntZero / (cntZero + cntNonZero) * 100);
            LOGGER.info("Count Non-Zero-Propability Sequences = %s (%6.2f%%)",
                    cntNonZero, (double) cntNonZero / (cntZero + cntNonZero)
                            * 100);
            LOGGER.info("Sum of Propabilities = %s", sumProbabilities);
            LOGGER.info("Cross Entropy = %s", crossEntropy);
            LOGGER.info("Entropy = %s", entropy);
        }
    }
}
