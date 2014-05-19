package de.typology.executables;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.typology.counting.AbsoluteCounter;
import de.typology.counting.ContinuationCounter;
import de.typology.extracting.TestSequenceExtractor;
import de.typology.indexing.Index;
import de.typology.indexing.IndexBuilder;
import de.typology.patterns.Pattern;
import de.typology.patterns.PatternElem;
import de.typology.sequencing.Sequencer;
import de.typology.smoothing.InterpolatedKneserNeySmoother;
import de.typology.splitting.DataSetSplitter;
import de.typology.tagging.PosTagger;
import de.typology.utils.Config;

public class KneserNeyBuilder {

    // TODO: share one ExecutorService across all stages

    private static Logger logger = LoggerFactory
            .getLogger(KneserNeyBuilder.class);

    private Config config;

    private KneserNeyBuilder(
            String[] args) throws IOException, InterruptedException {
        config = Config.get();
        loadStages(args);

        Path workingDir =
                Paths.get(config.outputDir).resolve(config.inputDataSet);
        Path trainingFile = workingDir.resolve("training.txt");
        Path taggedTrainingFile = workingDir.resolve("tagged-training.txt");
        Path indexFile = workingDir.resolve("index.txt");
        Path sequencesDir = workingDir.resolve("sequences");
        Path absoluteDir = workingDir.resolve("absolute");
        Path continuationDir = workingDir.resolve("continuation");
        Path testingSamplesDir = workingDir.resolve("testing-samples");

        if (config.splitData) {
            logger.info("split data");
            splitData(workingDir);
        }

        if (config.withPos) {
            tagTraining(trainingFile, taggedTrainingFile);
        }

        if (config.buildIndex) {
            logger.info("build word index");
            buildIndex(config.withPos ? taggedTrainingFile : trainingFile,
                    indexFile);
        }

        Index wordIndex;
        try (InputStream wordIndexInput = Files.newInputStream(indexFile)) {
            wordIndex = new Index(wordIndexInput);
        }

        if (config.buildSequences) {
            buildSequences(config.withPos ? taggedTrainingFile : trainingFile,
                    sequencesDir, wordIndex);
        }

        if (config.buildGLM) {
            buildGLM(sequencesDir, absoluteDir, wordIndex);
        }

        if (config.buildContinuationGLM) {
            buildContinuationGLM(absoluteDir, continuationDir, wordIndex);
        }

        if (config.extractContinuationGLM) {
            logger.info("extract continuation sequences");
            extractContinuationGLM(workingDir, absoluteDir, continuationDir,
                    testingSamplesDir);
        }

        if (config.buildKneserNey) {
            buildKneserNey(absoluteDir, continuationDir);
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

    private void tagTraining(Path trainingFile, Path taggedTrainingFile)
            throws IOException {
        PosTagger tagger =
                new PosTagger(trainingFile, taggedTrainingFile,
                        Paths.get(config.model));
        tagger.tag();
    }

    private void buildIndex(Path trainingFile, Path indexFile)
            throws IOException {
        try (InputStream input = Files.newInputStream(trainingFile);
                OutputStream output = Files.newOutputStream(indexFile)) {
            IndexBuilder indexBuilder =
                    new IndexBuilder(config.withPos, config.surroundWithTokens,
                            5);
            indexBuilder.buildIndex(input, output, config.maxWordCountDivider,
                    config.maxPosCountDivier);
        }
    }

    private void buildSequences(
            Path trainingFile,
            Path sequencesDir,
            Index wordIndex) throws IOException {
        Sequencer sequencer =
                new Sequencer(trainingFile, sequencesDir, wordIndex,
                        config.maxWordCountDivider, config.withPos,
                        config.surroundWithTokens);
        if (config.withPos) {
            sequencer.sequence(Pattern.getCombinations(config.modelLength,
                    new PatternElem[] {
                        PatternElem.CNT, PatternElem.SKP, PatternElem.POS
                    }));
        } else {
            sequencer.sequence(Pattern.getCombinations(config.modelLength,
                    new PatternElem[] {
                        PatternElem.CNT, PatternElem.SKP
                    }));
        }
    }

    private void buildGLM(Path sequencesDir, Path absoluteDir, Index wordIndex)
            throws IOException, InterruptedException {
        AbsoluteCounter absoluteCounter =
                new AbsoluteCounter(sequencesDir, absoluteDir, "\t",
                        config.numberOfCores, config.deleteTempFiles,
                        config.sortCounts);
        absoluteCounter.count();
    }

    private void buildContinuationGLM(
            Path absoluteDir,
            Path continuationDir,
            Index wordIndex) throws IOException, InterruptedException {
        ContinuationCounter continuationCounter =
                new ContinuationCounter(absoluteDir, continuationDir,
                        wordIndex, "\t", config.numberOfCores, config.withPos,
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

    private void buildKneserNey(Path absoluteDir, Path continuationDir)
            throws IOException {
        @SuppressWarnings("unused")
        InterpolatedKneserNeySmoother smoother =
                new InterpolatedKneserNeySmoother(absoluteDir, continuationDir,
                        "\t");
    }

    public static void main(String[] args) throws IOException,
            InterruptedException {
        new KneserNeyBuilder(args);
    }

}
