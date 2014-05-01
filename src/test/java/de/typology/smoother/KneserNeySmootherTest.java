package de.typology.smoother;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import de.typology.indexes.WordIndex;
import de.typology.indexes.WordIndexer;
import de.typology.patterns.PatternBuilder;
import de.typology.splitter.AbsoluteSplitter;
import de.typology.splitter.ContinuationSplitter;

public class KneserNeySmootherTest {

    File extractedSequenceDirectory;

    File absoluteDirectory;

    File continuationDirectory;

    File testSequenceFile;

    File kneserNeyFile;

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        String workingDirectoryPath = "testDataset/";
        File trainingFile = new File(workingDirectoryPath + "training.txt");
        File indexFile = new File(workingDirectoryPath + "index.txt");
        WordIndexer wier = new WordIndexer();
        wier.buildIndex(Files.newInputStream(trainingFile.toPath()),
                Files.newOutputStream(indexFile.toPath()), 10, "<fs> <s> ",
                " </s>");
        absoluteDirectory = new File(workingDirectoryPath + "absolute");
        continuationDirectory = new File(workingDirectoryPath + "continuation");

        WordIndex wordIndex = new WordIndex(new FileInputStream(indexFile));
        AbsoluteSplitter as =
                new AbsoluteSplitter(trainingFile.toPath(),
                        absoluteDirectory.toPath(), wordIndex, "\t",
                        "<fs> <s> ", " </s>", 2, true);
        as.split(PatternBuilder.getGLMForSmoothingPatterns(5));

        List<boolean[]> lmPatterns = PatternBuilder.getReverseLMPatterns(5);
        ContinuationSplitter smoothingSplitter =
                new ContinuationSplitter(absoluteDirectory,
                        continuationDirectory, indexFile, "\t", true);
        smoothingSplitter.split(lmPatterns, 2);

        testSequenceFile =
                new File(workingDirectoryPath + "test-sequences-5.txt");
        extractedSequenceDirectory = new File(workingDirectoryPath);
        absoluteDirectory = new File(workingDirectoryPath + "absolute");
        // TestSequenceExtractor tse = new TestSequenceExtractor(
        // this.testSequenceFile, this.absoluteDirectory,
        // this.continuationDirectory, this.extractedSequenceDirectory,
        // "\t", wi);
        // tse.extractContinuationSequences(5, 2);
        kneserNeyFile = new File(workingDirectoryPath + "kn-sequences-5.txt");
    }

    // @Test
    // public void calculateDiscoutValuesTest() {
    //
    // KneserNeySmoother kns = new KneserNeySmoother(
    // this.extractedSequenceDirectory, this.absoluteDirectory,
    // this.continuationDirectory, "\t", 5);
    // kns.smooth(this.testSequenceFile, this.kneserNeyFile, 5, false);
    // double d = kns.discountTypeValueMap.get("1").get("D1+");
    // assertEquals(0.529412, d, 0.00001);
    // }

    @Test
    public void calculateLowerOrderResultSimpleTest() {

        KneserNeySmoother kns =
                new KneserNeySmoother(extractedSequenceDirectory,
                        absoluteDirectory, continuationDirectory, "\t");

        HashMap<String, HashMap<String, Long>> absoluteTypeSequenceValueMap =
                null;
        HashMap<String, HashMap<String, Long[]>> continuationTypeSequenceValueMap =
                null;
        absoluteTypeSequenceValueMap =
                kns.readAbsoluteValuesIntoHashMap(kns.extractedAbsoluteDirectory);

        continuationTypeSequenceValueMap =
                kns.readContinuationValuesIntoHashMap(kns.extractedContinuationDirectory);

        kns.absoluteTypeSequenceValueMap = absoluteTypeSequenceValueMap;
        kns.continuationTypeSequenceValueMap = continuationTypeSequenceValueMap;

        kns.smooth(testSequenceFile, kneserNeyFile, 5, false, true);
        System.out.println(kns.continuationTypeSequenceValueMap.get("__").get(
                ""));
        assertEquals(0.625, kns.discountTypeValuesMap.get("_11").get("D1+"),
                0.00001);
        assertEquals(0.0357, kns.calculateLowerOrderResult("dolor", 1, "1"),
                0.0001);
        assertEquals(0.07143, kns.calculateLowerOrderResult("et", 1, "1"),
                0.0001);
        assertEquals(0.39282, kns.calculateLowerOrderResult("</s>", 1, "1"),
                0.0001);
        assertEquals(0.00840, kns.calculateLowerOrderResult("<s>", 1, "1"),
                0.0001);
        assertEquals(0.2098,
                kns.calculateLowerOrderResult("sit amet", 2, "11"), 0.0001);
        assertEquals(0.00525,
                kns.calculateLowerOrderResult("sit unknown", 2, "11"), 0.0001);
        assertEquals(0.309885,
                kns.calculateLowerOrderResult("dolor sit amet", 3, "111"),
                0.0001);
        assertEquals(0.3595, kns.calculateLowerOrderResult(
                "ipsum dolor sit amet", 4, "1111"), 0.0001);
        assertEquals(0.77929, kns.calculateConditionalProbability(
                "Lorem ipsum dolor sit amet", 5, "11111"), 0.0001);

    }

    @Test
    public void calculateLowerOrderResultComplexTest() {

        KneserNeySmoother kns =
                new KneserNeySmoother(extractedSequenceDirectory,
                        absoluteDirectory, continuationDirectory, "\t");

        HashMap<String, HashMap<String, Long>> absoluteTypeSequenceValueMap =
                null;
        HashMap<String, HashMap<String, Long[]>> continuationTypeSequenceValueMap =
                null;

        absoluteTypeSequenceValueMap =
                kns.readAbsoluteValuesIntoHashMap(kns.extractedAbsoluteDirectory);

        continuationTypeSequenceValueMap =
                kns.readContinuationValuesIntoHashMap(kns.extractedContinuationDirectory);

        kns.absoluteTypeSequenceValueMap = absoluteTypeSequenceValueMap;
        kns.continuationTypeSequenceValueMap = continuationTypeSequenceValueMap;

        kns.smooth(testSequenceFile, kneserNeyFile, 5, true, true);
        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        System.out.println("----");
        assertEquals(0.0084,
                kns.calculateConditionalProbability("notFound", 1, "1"), 0.0001);
        assertEquals(0.0084, kns.calculateLowerOrderResult("notFound", 1, "1"),
                0.0001);
        kns.calculateProbability("Lorem ipsum dolor sit amet", 5, "11111");
        assertEquals(0.625, kns.discountTypeValuesMap.get("_11").get("D1+"),
                0.00001);
        assertEquals(0.0357, kns.calculateLowerOrderResult("dolor", 1, "1"),
                0.0001);
        assertEquals(0.07143, kns.calculateLowerOrderResult("et", 1, "1"),
                0.0001);
        assertEquals(0.08474,
                kns.calculateConditionalProbability("et", 1, "1"), 0.0001);
        assertEquals(0.39282, kns.calculateLowerOrderResult("</s>", 1, "1"),
                0.0001);
        assertEquals(0.0084, kns.calculateLowerOrderResult("<s>", 1, "1"),
                0.0001);
        assertEquals(0.2321,
                kns.calculateLowerOrderResult("sit amet", 2, "11"), 0.0001);

        assertEquals(0.0275,
                kns.calculateLowerOrderResult("sit unknown", 2, "11"), 0.0001);
        assertEquals(0.3587,
                kns.calculateLowerOrderResult("dolor sit amet", 3, "111"),
                0.0001);
        assertEquals(0.4173, kns.calculateLowerOrderResult(
                "ipsum dolor sit amet", 4, "1111"), 0.0001);
        assertEquals(0.09857, kns.calculateConditionalProbability(
                "<s> At vero eos et", 5, "11111"), 0.0001);
        assertEquals(0.79221, kns.calculateConditionalProbability(
                "Lorem ipsum dolor sit amet", 5, "11111"), 0.0001);

        System.out.println(kns.calculateProbability(
                "Lorem ipsum dolor sit amet", 5, "11111"));
        // assertEquals(0.00875, kns.calculateProbability(
        // "Lorem ipsum dolor sit amet", 5, "11111"), 0.0001);
    }
}
