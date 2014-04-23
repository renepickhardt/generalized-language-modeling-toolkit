package de.typology.executables;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.typology.indexes.WordIndex;
import de.typology.indexes.WordIndexer;
import de.typology.patterns.PatternBuilder;
import de.typology.smoother.KneserNeySmoother;
import de.typology.smoother.ModifiedKneserNeySmoother;
import de.typology.splitter.AbsoluteSplitter;
import de.typology.splitter.DataSetSplitter;
import de.typology.splitter.SmoothingSplitter;
import de.typology.tester.TestSequenceExtractor;
import de.typology.utils.Config;

public class KneserNeyBuilder {

    static Logger logger = LogManager.getLogger(KneserNeyBuilder.class
            .getName());

    public static void main(String[] args) {
        File inputDirectory =
                new File(Config.get().outputDirectory
                        + Config.get().inputDataSet);
        File inputFile =
                new File(inputDirectory.getAbsolutePath() + "/training.txt");
        File indexFile =
                new File(inputDirectory.getAbsolutePath() + "/index.txt");
        File absoluteDirectory =
                new File(inputDirectory.getAbsolutePath() + "/absolute");
        File continuationDirectory =
                new File(inputDirectory.getAbsolutePath() + "/continuation");

        if (Config.get().splitData) {
            splitData(inputDirectory);
        }
        if (Config.get().buildIndex) {
            buildIndex(inputFile, indexFile);
        }
        if (Config.get().buildGLM) {
            buildGLM(inputFile, indexFile, absoluteDirectory);
        }
        if (Config.get().buildContinuationGLM) {
            buildContinuationGLM(inputFile, indexFile, absoluteDirectory,
                    continuationDirectory);
        }

        File testExtractOutputDirectory =
                new File(inputDirectory.getAbsolutePath() + "/testing-samples");
        if (Config.get().extractContinuationGLM) {
            extractContinuationGLM(inputDirectory, indexFile,
                    absoluteDirectory, continuationDirectory,
                    testExtractOutputDirectory);

        }

        if (Config.get().buildKneserNey) {
            KneserNeySmoother kns =
                    buildKneserNey(inputDirectory, absoluteDirectory,
                            continuationDirectory, testExtractOutputDirectory);
            if (Config.get().buildModKneserNey) {
                buildModKneserNey(inputDirectory, absoluteDirectory,
                        continuationDirectory, testExtractOutputDirectory,
                        kns.absoluteTypeSequenceValueMap,
                        kns.continuationTypeSequenceValueMap);
            }
        }
        logger.info("done");
    }

    private static void splitData(File inputDirectory) {
        DataSetSplitter dss =
                new DataSetSplitter(inputDirectory, "normalized.txt");
        dss.split("training.txt", "learning.txt", "testing.txt",
                Config.get().modelLength);
        dss.splitIntoSequences(new File(inputDirectory.getAbsolutePath()
                + "/testing.txt"), Config.get().modelLength,
                Config.get().numberOfQueries);
    }

    private static void buildIndex(File inputFile, File indexFile) {
        logger.info("build word index: " + indexFile.getAbsolutePath());
        WordIndexer wordIndexer = new WordIndexer();
        wordIndexer.buildIndex(inputFile, indexFile,
                Config.get().maxCountDivider, "<fs> <s> ", " </s>");
    }

    private static void buildGLM(
            File inputFile,
            File indexFile,
            File absoluteDirectory) {
        ArrayList<boolean[]> glmForSmoothingPatterns =
                PatternBuilder
                        .getReverseGLMForSmoothingPatterns(Config.get().modelLength);
        AbsoluteSplitter absolteSplitter =
                new AbsoluteSplitter(inputFile, indexFile, absoluteDirectory,
                        "\t", Config.get().deleteTempFiles, "<fs> <s> ",
                        " </s>");
        logger.info("split into GLM sequences: " + inputFile.getAbsolutePath());
        absolteSplitter.split(glmForSmoothingPatterns,
                Config.get().numberOfCores);
    }

    private static void buildContinuationGLM(
            File inputFile,
            File indexFile,
            File absoluteDirectory,
            File continuationDirectory) {
        ArrayList<boolean[]> lmPatterns =
                PatternBuilder.getReverseLMPatterns(Config.get().modelLength);
        SmoothingSplitter smoothingSplitter =
                new SmoothingSplitter(absoluteDirectory, continuationDirectory,
                        indexFile, "\t", Config.get().deleteTempFiles);
        logger.info("split into continuation sequences: "
                + inputFile.getAbsolutePath());
        smoothingSplitter.split(lmPatterns, Config.get().numberOfCores);
    }

    private static void extractContinuationGLM(
            File inputDirectory,
            File indexFile,
            File absoluteDirectory,
            File continuationDirectory,
            File testExtractOutputDirectory) {
        File testSequences =
                new File(inputDirectory.getAbsolutePath() + "/testing-samples-"
                        + Config.get().modelLength + ".txt");
        testExtractOutputDirectory.mkdir();

        TestSequenceExtractor tse =
                new TestSequenceExtractor(testSequences, absoluteDirectory,
                        continuationDirectory, testExtractOutputDirectory,
                        "\t", new WordIndex(indexFile));
        tse.extractSequences(Config.get().modelLength,
                Config.get().numberOfCores);
        tse.extractContinuationSequences(Config.get().modelLength,
                Config.get().numberOfCores);
    }

    private static KneserNeySmoother buildKneserNey(
            File inputDirectory,
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

        for (int i = Config.get().modelLength; i >= 1; i--) {
            File inputSequenceFile =
                    new File(inputDirectory.getAbsolutePath()
                            + "/testing-samples-" + i + ".txt");
            File resultFile;
            // smooth simple
            if (Config.get().kneserNeySimple) {
                resultFile =
                        new File(inputDirectory.getAbsolutePath()
                                + "/kneser-ney-simple-backoffToCont-" + i
                                + ".txt");
                kns.smooth(inputSequenceFile, resultFile, i, false,
                        Config.get().conditionalProbabilityOnly);
            }
            // smooth complex
            if (Config.get().kneserNeyComplex) {
                resultFile =
                        new File(inputDirectory.getAbsolutePath()
                                + "/kneser-ney-complex-backoffToCont-" + i
                                + ".txt");
                kns.smooth(inputSequenceFile, resultFile, i, true,
                        Config.get().conditionalProbabilityOnly);
            }
        }
        return kns;
    }

    private static
        void
        buildModKneserNey(
                File inputDirectory,
                File absoluteDirectory,
                File continuationDirectory,
                File testExtractOutputDirectory,
                HashMap<String, HashMap<String, Long>> absoluteTypeSequenceValueMap,
                HashMap<String, HashMap<String, Long[]>> continuationTypeSequenceValueMap) {
        ModifiedKneserNeySmoother mkns =
                new ModifiedKneserNeySmoother(testExtractOutputDirectory,
                        absoluteDirectory, continuationDirectory, "\t",
                        Config.get().decimalPlaces);

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

        for (int i = Config.get().modelLength; i >= 1; i--) {
            File inputSequenceFile =
                    new File(inputDirectory.getAbsolutePath()
                            + "/testing-samples-" + i + ".txt");
            File resultFile;
            // smooth simple
            if (Config.get().kneserNeySimple) {
                resultFile =
                        new File(inputDirectory.getAbsolutePath()
                                + "/mod-kneser-ney-simple-backoffToCont-" + i
                                + ".txt");
                mkns.smooth(inputSequenceFile, resultFile, i, false,
                        Config.get().conditionalProbabilityOnly);
            }
            // smooth complex
            if (Config.get().kneserNeyComplex) {
                resultFile =
                        new File(inputDirectory.getAbsolutePath()
                                + "/mod-kneser-ney-complex-backoffToCont-" + i
                                + ".txt");
                mkns.smooth(inputSequenceFile, resultFile, i, true,
                        Config.get().conditionalProbabilityOnly);
            }
        }
    }
}
