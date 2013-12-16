package de.typology.smoother;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import de.typology.indexes.WordIndex;
import de.typology.indexes.WordIndexer;
import de.typology.patterns.PatternBuilder;
import de.typology.splitter.AbsoluteSplitter;
import de.typology.tester.TestSequenceExtractor;

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
		WordIndex wi = new WordIndex(indexFile);
		this.absoluteDirectory = new File(inputDirectoryPath + "absolute");

		AbsoluteSplitter as = new AbsoluteSplitter(inputFile, indexFile,
				this.absoluteDirectory, "\t", true, "<fs> <s> ", " </s>");
		as.split(PatternBuilder.getGLMForSmoothingPatterns(5), 2);

		this.testSequenceFile = new File(inputDirectoryPath
				+ "test-sequences-5.txt");
		this.extractedSequenceDirectory = new File(inputDirectoryPath);
		this.absoluteDirectory = new File(inputDirectoryPath + "absolute");
		this.continuationDirectory = new File(inputDirectoryPath
				+ "continuation");
		TestSequenceExtractor tse = new TestSequenceExtractor(
				this.testSequenceFile, this.absoluteDirectory,
				this.extractedSequenceDirectory, "\t", wi);
		tse.extractContinuationSequences(5, 2);
		this.kneserNeyFile = new File(inputDirectoryPath + "kn-sequences-5.txt");
	}

	@After
	public void tearDown() throws Exception {
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
	public void calculateLowerOrderResultTest() {

		KneserNeySmoother kns = new KneserNeySmoother(
				this.extractedSequenceDirectory, this.absoluteDirectory,
				this.continuationDirectory, "\t", 5);
		kns.smooth(this.testSequenceFile, this.kneserNeyFile, 5, false);
		System.out.println(kns.continuationTypeSequenceValueMap.get("__").get(
				""));
		assertEquals(0.0357, kns.calculateLowerOrderResult("dolor", 1, "1"),
				0.0001);
		assertEquals(0.07143, kns.calculateLowerOrderResult("et", 1, "1"),
				0.0001);
		assertEquals(0.39282, kns.calculateLowerOrderResult("</s>", 1, "1"),
				0.0001);
		assertEquals(0.0, kns.calculateLowerOrderResult("<s>", 1, "1"), 0.0001);

		// assertEquals(0.2098,
		// kns.calculateLowerOrderResult("ipsum dolor", 2, "11"), 0.0001);

	}

}
