package de.typology.smoother;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.ArrayList;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import de.typology.indexes.WordIndexer;
import de.typology.patterns.PatternBuilder;
import de.typology.splitter.AbsoluteSplitter;
import de.typology.splitter.SmoothingSplitter;

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
		String inputDirectoryPath = "testDataset/";
		File inputFile = new File(inputDirectoryPath + "training.txt");
		File indexFile = new File(inputDirectoryPath + "index.txt");
		WordIndexer wier = new WordIndexer();
		wier.buildIndex(inputFile, indexFile, 10, "<fs> <s> ", " </s>");
		this.absoluteDirectory = new File(inputDirectoryPath + "absolute");
		this.continuationDirectory = new File(inputDirectoryPath
				+ "continuation");

		AbsoluteSplitter as = new AbsoluteSplitter(inputFile, indexFile,
				this.absoluteDirectory, "\t", true, "<fs> <s> ", " </s>");
		as.split(PatternBuilder.getGLMForSmoothingPatterns(5), 2);

		ArrayList<boolean[]> lmPatterns = PatternBuilder
				.getReverseLMPatterns(5);
		SmoothingSplitter smoothingSplitter = new SmoothingSplitter(
				this.absoluteDirectory, this.continuationDirectory, indexFile,
				"\t", true);
		smoothingSplitter.split(lmPatterns, 2);

		this.testSequenceFile = new File(inputDirectoryPath
				+ "test-sequences-5.txt");
		this.extractedSequenceDirectory = new File(inputDirectoryPath);
		this.absoluteDirectory = new File(inputDirectoryPath + "absolute");
		// TestSequenceExtractor tse = new TestSequenceExtractor(
		// this.testSequenceFile, this.absoluteDirectory,
		// this.continuationDirectory, this.extractedSequenceDirectory,
		// "\t", wi);
		// tse.extractContinuationSequences(5, 2);
		this.kneserNeyFile = new File(inputDirectoryPath + "kn-sequences-5.txt");
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

		KneserNeySmoother kns = new KneserNeySmoother(
				this.extractedSequenceDirectory, this.absoluteDirectory,
				this.continuationDirectory, "\t", 5);
		kns.smooth(this.testSequenceFile, this.kneserNeyFile, 5, false, true,
				false);
		System.out.println(kns.continuationTypeSequenceValueMap.get("__").get(
				""));
		assertEquals(0.625, kns.discountTypeValuesMap.get("_11").get("D1+"),
				0.00001);
		assertEquals(0.0357,
				kns.calculateLowerOrderResult("dolor", 1, "1", false), 0.0001);
		assertEquals(0.07143,
				kns.calculateLowerOrderResult("et", 1, "1", false), 0.0001);
		assertEquals(0.39282,
				kns.calculateLowerOrderResult("</s>", 1, "1", false), 0.0001);
		assertEquals(0.0, kns.calculateLowerOrderResult("<s>", 1, "1", false),
				0.0001);
		assertEquals(0.2098,
				kns.calculateLowerOrderResult("sit amet", 2, "11", false),
				0.0001);
		assertEquals(0.309885, kns.calculateLowerOrderResult("dolor sit amet",
				3, "111", false), 0.0001);
		assertEquals(0.3595, kns.calculateLowerOrderResult(
				"ipsum dolor sit amet", 4, "1111", false), 0.0001);
		assertEquals(0.77929, kns.calculateConditionalProbability(
				"Lorem ipsum dolor sit amet", 5, "11111", false), 0.0001);

	}

	// @Test
	// public void calculateLowerOrderResultComplexTest() {
	//
	// KneserNeySmoother kns = new KneserNeySmoother(
	// this.extractedSequenceDirectory, this.absoluteDirectory,
	// this.continuationDirectory, "\t", 5);
	// kns.smooth(this.testSequenceFile, this.kneserNeyFile, 5, true, true,
	// false);
	// try {
	// Thread.sleep(500);
	// } catch (InterruptedException ex) {
	// Thread.currentThread().interrupt();
	// }
	// assertEquals(0.625, kns.discountTypeValuesMap.get("_11").get("D1+"),
	// 0.00001);
	// assertEquals(0.0357,
	// kns.calculateLowerOrderResult("dolor", 1, "1", false), 0.0001);
	// assertEquals(0.07143,
	// kns.calculateLowerOrderResult("et", 1, "1", false), 0.0001);
	// assertEquals(0.07246,
	// kns.calculateConditionalProbability("et", 1, "1", false),
	// 0.0001);
	// assertEquals(0.39282,
	// kns.calculateLowerOrderResult("</s>", 1, "1", false), 0.0001);
	// assertEquals(0.0, kns.calculateLowerOrderResult("<s>", 1, "1", false),
	// 0.0001);
	// assertEquals(0.20982,
	// kns.calculateLowerOrderResult("sit amet", 2, "11", false),
	// 0.0001);
	// assertEquals(0.26454, kns.calculateLowerOrderResult("dolor sit amet",
	// 3, "111", false), 0.0001);
	// assertEquals(0.249458, kns.calculateLowerOrderResult(
	// "ipsum dolor sit amet", 4, "1111", false), 0.0001);
	// assertEquals(0.06944, kns.calculateConditionalProbability(
	// "<s> At vero eos et", 5, "11111", false), 0.0001);
	// assertEquals(0.74906, kns.calculateConditionalProbability(
	// "Lorem ipsum dolor sit amet", 5, "11111", false), 0.0001);
	//
	// assertEquals(0.00875, kns.calculateProbability(
	// "Lorem ipsum dolor sit amet", 5, "11111", false), 0.0001);
	// }
}
