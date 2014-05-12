package de.typology.executables;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.typology.counting.AbsoluteCounter;
import de.typology.counting.ContinuationCounter;
import de.typology.extracting.TestSequenceExtractor;
import de.typology.indexing.WordIndex;
import de.typology.indexing.WordIndexBuilder;
import de.typology.patterns.Pattern;
import de.typology.patterns.PatternElem;
import de.typology.sequencing.Sequencer;
import de.typology.smoothing.LegacyKneserNeySmoother;
import de.typology.smoothing.LegacyModifiedKneserNeySmoother;
import de.typology.splitting.DataSetSplitter;
import de.typology.utils.Config;

public class KneserNeyBuilder {

    // TODO: share one ExecutorService across all stages

    private static Logger logger = LogManager.getLogger();

    private Config config;

    private KneserNeyBuilder(
            String[] args) throws IOException, InterruptedException {
        config = Config.get();
        loadStages(args);

        Path workingDir =
                Paths.get(config.outputDir).resolve(config.inputDataSet);
        Path trainingFile = workingDir.resolve("training.txt");
        Path indexFile = workingDir.resolve("index.txt");
        Path sequencesDir = workingDir.resolve("sequences");
        Path absoluteDir = workingDir.resolve("absolute");
        Path continuationDir = workingDir.resolve("continuation");
        Path testingSamplesDir = workingDir.resolve("testing-samples");

        if (config.splitData) {
            logger.info("split data");
            splitData(workingDir);
        }

        if (config.buildIndex) {
            logger.info("build word index");
            buildIndex(trainingFile, indexFile);
        }

        WordIndex wordIndex;
        try (InputStream wordIndexInput = Files.newInputStream(indexFile)) {
            wordIndex = new WordIndex(wordIndexInput);
        }

        if (config.buildSequences) {
            buildSequences(trainingFile, sequencesDir, wordIndex);
        }

        if (config.buildGLM) {
            buildGLM(sequencesDir, absoluteDir, wordIndex);
        }

        if (config.buildContinuationGLM) {
            buildContinuationGLM(trainingFile, wordIndex, absoluteDir,
                    continuationDir);
        }

        if (config.extractContinuationGLM) {
            logger.info("extract continuation sequences");
            extractContinuationGLM(workingDir, absoluteDir, continuationDir,
                    testingSamplesDir);
        }

        if (config.buildKneserNey) {
            logger.info("build kneser ney");
            LegacyKneserNeySmoother kns =
                    buildKneserNey(workingDir, absoluteDir, continuationDir,
                            testingSamplesDir);

            if (config.buildModKneserNey) {
                logger.info("build modified kneser ney");
                buildModKneserNey(workingDir, absoluteDir, continuationDir,
                        testingSamplesDir, kns.absoluteTypeSequenceValueMap,
                        kns.continuationTypeSequenceValueMap);
            }
        }

        logger.info("done");
    }

    private void loadStages(String[] stages) {
        if (stages.length == 0) {
            return;
        }

        config.splitData = false;
        config.buildIndex = false;
        config.buildSequences = false;
        config.buildGLM = false;
        config.buildContinuationGLM = false;
        config.extractContinuationGLM = false;
        config.buildKneserNey = false;
        config.buildModKneserNey = false;

        if (stages.length == 1) {
            if (stages[0].equals("none")) {
                return;
            } else if (stages[0].equals("all")) {
                config.splitData = true;
                config.buildIndex = true;
                config.buildSequences = true;
                config.buildGLM = true;
                config.buildContinuationGLM = true;
                config.extractContinuationGLM = true;
                config.buildKneserNey = true;
                config.buildModKneserNey = true;
                return;
            }
        }

        for (String stage : stages) {
            switch (stage) {
                case "split":
                    config.splitData = true;
                    break;

                case "index":
                    config.buildIndex = true;
                    break;

                case "sequences":
                    config.buildSequences = true;
                    break;

                case "glm":
                    config.buildGLM = true;
                    break;

                case "contglm":
                    config.buildContinuationGLM = true;
                    break;

                case "extract":
                    config.extractContinuationGLM = true;
                    break;

                case "kneserney":
                    config.buildKneserNey = true;
                    break;

                case "modkneserney":
                    config.buildModKneserNey = true;
                    break;

                default:
                    throw new IllegalArgumentException("Unkown stage: " + stage);
            }
        }
    }

    private void splitData(Path workingDir) throws IOException {
        DataSetSplitter dss =
                new DataSetSplitter(workingDir.toFile(), "normalized.txt");
        dss.split("training.txt", "learning.txt", "testing.txt");
        dss.splitIntoSequences(workingDir.resolve("testing.txt").toFile(),
                config.modelLength, config.numberOfQueries);
    }

    private void buildIndex(Path trainingFile, Path indexFile)
            throws IOException {
        try (InputStream input = Files.newInputStream(trainingFile);
                OutputStream output = Files.newOutputStream(indexFile)) {
            WordIndexBuilder wordIndexer = new WordIndexBuilder();
            wordIndexer.buildIndex(input, output, config.maxCountDivider,
                    "<fs>/<fs> <bos>/<bos> ", " <eos>/<eos>");
        }
    }

    private void buildSequences(
            Path trainingFile,
            Path sequencesDir,
            WordIndex wordIndex) throws IOException {
        Sequencer sequencer =
                new Sequencer(trainingFile, sequencesDir, wordIndex,
                        config.maxCountDivider, config.surroundWithTokens);
        sequencer.sequence(Pattern.getCombinations(config.modelLength,
                new PatternElem[] {
                    PatternElem.CNT, PatternElem.SKP, PatternElem.POS
                }));
    }

    private void buildGLM(
            Path sequencesDir,
            Path absoluteDir,
            WordIndex wordIndex) throws IOException, InterruptedException {
        AbsoluteCounter absoluteCounter =
                new AbsoluteCounter(sequencesDir, absoluteDir, "\t",
                        config.numberOfCores, config.deleteTempFiles,
                        config.sortCounts);
        absoluteCounter.count();
    }

    private void buildContinuationGLM(
            Path trainingFile,
            WordIndex wordIndex,
            Path absoluteDir,
            Path continuationDir) throws IOException, InterruptedException {
        ContinuationCounter continuationCounter =
                new ContinuationCounter(absoluteDir, continuationDir,
                        wordIndex, "\t", config.numberOfCores,
                        config.sortCounts);
        continuationCounter.count();
    }

    private void extractContinuationGLM(
            Path workingDir,
            Path absoluteDir,
            Path continuationDir,
            Path testExtractOutputDir) throws IOException, InterruptedException {
        try (InputStream input =
                Files.newInputStream(workingDir.resolve("testing-samples-"
                        + config.modelLength + ".txt"))) {
            TestSequenceExtractor testSequenceExtractor =
                    new TestSequenceExtractor(input, absoluteDir,
                            continuationDir, testExtractOutputDir, "\t",
                            config.modelLength, config.numberOfCores);
            testSequenceExtractor.extractAbsoluteSequences();
            testSequenceExtractor.extractContinuationSequences();
        }
    }

    private LegacyKneserNeySmoother buildKneserNey(
            Path workingDir,
            Path absoluteDir,
            Path continuationDir,
            Path testExtractOutputDir) {
        LegacyKneserNeySmoother knewserNeySmoother =
                new LegacyKneserNeySmoother(testExtractOutputDir.toFile(),
                        absoluteDir.toFile(), continuationDir.toFile(), "\t");

        // read absolute and continuation values into HashMaps
        logger.info("read absolute and continuation values into HashMaps for kneser ney");
        knewserNeySmoother.absoluteTypeSequenceValueMap =
                knewserNeySmoother
                        .readAbsoluteValuesIntoHashMap(knewserNeySmoother.extractedAbsoluteDir);
        knewserNeySmoother.continuationTypeSequenceValueMap =
                knewserNeySmoother
                        .readContinuationValuesIntoHashMap(knewserNeySmoother.extractedContinuationDir);

        for (int i = config.modelLength; i >= 1; i--) {
            Path inputSequenceFile =
                    workingDir.resolve("testing-samples-" + i + ".txt");

            if (config.kneserNeySimple) {
                Path resultFile =
                        workingDir.resolve("kneser-ney-simple-backoffToCont-"
                                + i + ".txt");
                knewserNeySmoother.smooth(inputSequenceFile.toFile(),
                        resultFile.toFile(), i, false,
                        config.conditionalProbabilityOnly);
            }

            if (config.kneserNeyComplex) {
                Path resultFile =
                        workingDir.resolve("kneser-ney-complex-backoffToCont-"
                                + i + ".txt");
                knewserNeySmoother.smooth(inputSequenceFile.toFile(),
                        resultFile.toFile(), i, true,
                        config.conditionalProbabilityOnly);
            }
        }

        return knewserNeySmoother;
    }

    private
        void
        buildModKneserNey(
                Path workingDir,
                Path absoluteDir,
                Path continuationDir,
                Path testExtractOutputDir,
                HashMap<String, HashMap<String, Long>> absoluteTypeSequenceValueMap,
                HashMap<String, HashMap<String, Long[]>> continuationTypeSequenceValueMap) {
        LegacyModifiedKneserNeySmoother modifiedKneserNeySmoother =
                new LegacyModifiedKneserNeySmoother(testExtractOutputDir.toFile(),
                        absoluteDir.toFile(), continuationDir.toFile(), "\t",
                        config.decimalPlaces);

        if (absoluteTypeSequenceValueMap == null) {
            // read absolute and continuation values into HashMaps

            logger.info("read absolute and continuation values into HashMaps for mod kneser ney");
            absoluteTypeSequenceValueMap =
                    modifiedKneserNeySmoother
                            .readAbsoluteValuesIntoHashMap(modifiedKneserNeySmoother.extractedAbsoluteDir);

            continuationTypeSequenceValueMap =
                    modifiedKneserNeySmoother
                            .readContinuationValuesIntoHashMap(modifiedKneserNeySmoother.extractedContinuationDir);
        }

        modifiedKneserNeySmoother.absoluteTypeSequenceValueMap =
                absoluteTypeSequenceValueMap;
        modifiedKneserNeySmoother.continuationTypeSequenceValueMap =
                continuationTypeSequenceValueMap;

        for (int i = config.modelLength; i >= 1; i--) {
            Path inputSequenceFile =
                    workingDir.resolve("testing-samples-" + i + ".txt");

            if (config.kneserNeySimple) {
                Path resultFile =
                        workingDir
                                .resolve("mod-kneser-ney-simple-backoffToCont-"
                                        + i + ".txt");
                modifiedKneserNeySmoother.smooth(inputSequenceFile.toFile(),
                        resultFile.toFile(), i, false,
                        config.conditionalProbabilityOnly);
            }

            if (config.kneserNeyComplex) {
                Path resultFile =
                        workingDir
                                .resolve("mod-kneser-ney-complex-backoffToCont-"
                                        + i + ".txt");
                modifiedKneserNeySmoother.smooth(inputSequenceFile.toFile(),
                        resultFile.toFile(), i, true,
                        config.conditionalProbabilityOnly);
            }
        }
    }

    public static void main(String[] args) throws IOException,
            InterruptedException {
        new KneserNeyBuilder(args);
    }

}
