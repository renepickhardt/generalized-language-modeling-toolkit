package de.typology.executables;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.typology.indexes.WordIndex;
import de.typology.indexes.WordIndexer;
import de.typology.patterns.PatternBuilder;
import de.typology.smoother.KneserNeySmoother;
import de.typology.smoother.ModifiedKneserNeySmoother;
import de.typology.splitter.AbsoluteSplitter;
import de.typology.splitter.ContinuationSplitter;
import de.typology.splitter.DataSetSplitter;
import de.typology.tester.TestSequenceExtractor;
import de.typology.utils.Config;

public class KneserNeyBuilder {

    private static Logger logger = LogManager.getLogger(KneserNeyBuilder.class
            .getName());

    private Config config;

    private KneserNeyBuilder(
            String[] args) throws IOException, InterruptedException {
        config = Config.get();
        loadStages(args);

        File workingDirectory =
                new File(config.outputDirectory + config.inputDataSet);
        File trainingFile =
                new File(workingDirectory.getAbsolutePath() + "/training.txt");
        File indexFile =
                new File(workingDirectory.getAbsolutePath() + "/index.txt");
        File absoluteDirectory =
                new File(workingDirectory.getAbsolutePath() + "/absolute");
        File continuationDirectory =
                new File(workingDirectory.getAbsolutePath() + "/continuation");

        if (config.splitData) {
            logger.info("split data");
            splitData(workingDirectory);
        }
        if (config.buildIndex) {
            logger.info("build word index");
            buildIndex(trainingFile, indexFile);
        }
        if (config.buildGLM) {
            logger.info("split into GLM sequences");
            buildGLM(trainingFile, indexFile, absoluteDirectory);
        }
        if (config.buildContinuationGLM) {
            logger.info("split into continuation sequences");
            buildContinuationGLM(trainingFile, indexFile, absoluteDirectory,
                    continuationDirectory);
        }

        File testExtractOutputDirectory =
                new File(workingDirectory.getAbsolutePath()
                        + "/testing-samples");
        if (config.extractContinuationGLM) {
            logger.info("extract continuation sequences");
            extractContinuationGLM(workingDirectory, indexFile,
                    absoluteDirectory, continuationDirectory,
                    testExtractOutputDirectory);

        }

        if (config.buildKneserNey) {
            logger.info("build kneser ney");
            KneserNeySmoother kns =
                    buildKneserNey(workingDirectory, absoluteDirectory,
                            continuationDirectory, testExtractOutputDirectory);

            if (config.buildModKneserNey) {
                logger.info("build modified kneser ney");
                buildModKneserNey(workingDirectory, absoluteDirectory,
                        continuationDirectory, testExtractOutputDirectory,
                        kns.absoluteTypeSequenceValueMap,
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

    private void splitData(File workingDirectory) throws IOException {
        DataSetSplitter dss =
                new DataSetSplitter(workingDirectory, "normalized.txt");
        dss.split("training.txt", "learning.txt", "testing.txt");
        dss.splitIntoSequences(new File(workingDirectory.getAbsolutePath()
                + "/testing.txt"), config.modelLength, config.numberOfQueries);
    }

    private void buildIndex(File trainingFile, File indexFile)
            throws IOException {
        InputStream input = Files.newInputStream(trainingFile.toPath());
        OutputStream output = Files.newOutputStream(indexFile.toPath());
        WordIndexer wordIndexer = new WordIndexer();
        wordIndexer.buildIndex(input, output, config.maxCountDivider,
                "<fs> <s> ", " </s>");
    }

    private void buildGLM(
            File trainingFile,
            File indexFile,
            File absoluteDirectory) throws IOException, InterruptedException {
        List<boolean[]> glmForSmoothingPatterns =
                PatternBuilder
                        .getReverseGLMForSmoothingPatterns(config.modelLength);
        WordIndex wordIndex = new WordIndex(new FileInputStream(indexFile));
        AbsoluteSplitter absolteSplitter =
                new AbsoluteSplitter(trainingFile.toPath(),
                        absoluteDirectory.toPath(), wordIndex, "\t",
                        "<fs> <s> ", " </s>", config.numberOfCores,
                        config.deleteTempFiles);
        absolteSplitter.split(glmForSmoothingPatterns);
    }

    private void buildContinuationGLM(
            File trainingFile,
            File indexFile,
            File absoluteDirectory,
            File continuationDirectory) throws IOException,
            InterruptedException {
        List<boolean[]> lmPatterns =
                PatternBuilder.getReverseLMPatterns(config.modelLength);
        ContinuationSplitter smoothingSplitter =
                new ContinuationSplitter(absoluteDirectory,
                        continuationDirectory, indexFile, "\t",
                        config.deleteTempFiles);
        smoothingSplitter.split(lmPatterns, config.numberOfCores);
    }

    private void extractContinuationGLM(
            File workingDirectory,
            File indexFile,
            File absoluteDirectory,
            File continuationDirectory,
            File testExtractOutputDirectory) throws IOException {
        File testSequences =
                new File(workingDirectory.getAbsolutePath()
                        + "/testing-samples-" + config.modelLength + ".txt");
        testExtractOutputDirectory.mkdir();

        TestSequenceExtractor tse =
                new TestSequenceExtractor(testSequences, absoluteDirectory,
                        continuationDirectory, testExtractOutputDirectory,
                        "\t", new WordIndex(new FileInputStream(indexFile)));
        tse.extractSequences(config.modelLength, config.numberOfCores);
        tse.extractContinuationSequences(config.modelLength,
                config.numberOfCores);
    }

    private KneserNeySmoother buildKneserNey(
            File workingDirectory,
            File absoluteDirectory,
            File continuationDirectory,
            File testExtractOutputDirectory) {
        KneserNeySmoother kns;
        kns =
                new KneserNeySmoother(testExtractOutputDirectory,
                        absoluteDirectory, continuationDirectory, "\t");

        // read absolute and continuation values into HashMaps
        logger.info("read absolute and continuation values into HashMaps for kneser ney");
        kns.absoluteTypeSequenceValueMap =
                kns.readAbsoluteValuesIntoHashMap(kns.extractedAbsoluteDirectory);
        kns.continuationTypeSequenceValueMap =
                kns.readContinuationValuesIntoHashMap(kns.extractedContinuationDirectory);

        for (int i = config.modelLength; i >= 1; i--) {
            File inputSequenceFile =
                    new File(workingDirectory.getAbsolutePath()
                            + "/testing-samples-" + i + ".txt");
            File resultFile;
            // smooth simple
            if (config.kneserNeySimple) {
                resultFile =
                        new File(workingDirectory.getAbsolutePath()
                                + "/kneser-ney-simple-backoffToCont-" + i
                                + ".txt");
                kns.smooth(inputSequenceFile, resultFile, i, false,
                        config.conditionalProbabilityOnly);
            }
            // smooth complex
            if (config.kneserNeyComplex) {
                resultFile =
                        new File(workingDirectory.getAbsolutePath()
                                + "/kneser-ney-complex-backoffToCont-" + i
                                + ".txt");
                kns.smooth(inputSequenceFile, resultFile, i, true,
                        config.conditionalProbabilityOnly);
            }
        }
        return kns;
    }

    private
        void
        buildModKneserNey(
                File workingDirectory,
                File absoluteDirectory,
                File continuationDirectory,
                File testExtractOutputDirectory,
                HashMap<String, HashMap<String, Long>> absoluteTypeSequenceValueMap,
                HashMap<String, HashMap<String, Long[]>> continuationTypeSequenceValueMap) {
        ModifiedKneserNeySmoother mkns =
                new ModifiedKneserNeySmoother(testExtractOutputDirectory,
                        absoluteDirectory, continuationDirectory, "\t",
                        config.decimalPlaces);

        if (absoluteTypeSequenceValueMap == null) {
            // read absolute and continuation values into HashMaps

            logger.info("read absolute and continuation values into HashMaps for mod kneser ney");
            absoluteTypeSequenceValueMap =
                    mkns.readAbsoluteValuesIntoHashMap(mkns.extractedAbsoluteDirectory);

            continuationTypeSequenceValueMap =
                    mkns.readContinuationValuesIntoHashMap(mkns.extractedContinuationDirectory);
        }

        mkns.absoluteTypeSequenceValueMap = absoluteTypeSequenceValueMap;
        mkns.continuationTypeSequenceValueMap =
                continuationTypeSequenceValueMap;

        for (int i = config.modelLength; i >= 1; i--) {
            File inputSequenceFile =
                    new File(workingDirectory.getAbsolutePath()
                            + "/testing-samples-" + i + ".txt");
            File resultFile;
            // smooth simple
            if (config.kneserNeySimple) {
                resultFile =
                        new File(workingDirectory.getAbsolutePath()
                                + "/mod-kneser-ney-simple-backoffToCont-" + i
                                + ".txt");
                mkns.smooth(inputSequenceFile, resultFile, i, false,
                        config.conditionalProbabilityOnly);
            }
            // smooth complex
            if (config.kneserNeyComplex) {
                resultFile =
                        new File(workingDirectory.getAbsolutePath()
                                + "/mod-kneser-ney-complex-backoffToCont-" + i
                                + ".txt");
                mkns.smooth(inputSequenceFile, resultFile, i, true,
                        config.conditionalProbabilityOnly);
            }
        }
    }

    public static void main(String[] args) throws IOException,
            InterruptedException {
        new KneserNeyBuilder(args);
    }

}