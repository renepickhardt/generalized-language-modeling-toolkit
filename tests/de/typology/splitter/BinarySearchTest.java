/**
 * 
 */
package de.typology.splitter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.typology.testutils.Resetter;
import de.typology.utils.Config;

/**
 * @author mkoerner
 * 
 */
public class BinarySearchTest {
	private String outputDirectory;
	IndexBuilder ib;
	private BufferedReader normalizedReader;
	private String[] wordIndex;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		// reset testDataset/
		Resetter.reset("testDataset/");

		// modify configuration parameters
		Config.get().maxCountDivider = 9;
		Config.get().minCountPerFile = 2;
		Config.get().fileSizeThreashhold = 100;

		// build index
		IndexBuilder ib = new IndexBuilder();
		String outputDirectory = "testDataset/";
		ib.buildIndex(outputDirectory + "normalized.txt", outputDirectory
				+ "index.txt", outputDirectory + "stats.txt");

		// build ngrams
		NGramSplitter ngs = new NGramSplitter(outputDirectory, "index.txt",
				"stats.txt", "normalized.txt");
		ngs.split(5);

	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		// remove the following part to keep the generated output
		Resetter.reset("testDataset/");
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		this.outputDirectory = "testDataset/";
		this.normalizedReader = new BufferedReader(new FileReader(
				this.outputDirectory + "normalized.txt"));
		this.ib = new IndexBuilder();
		this.wordIndex = this.ib.deserializeIndex(this.outputDirectory
				+ "index.txt");
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		this.normalizedReader.close();
	}

	// tests if all results are inside the bounds of wordIndex
	@Test
	public void testRankTwoParametersAll() throws IOException {
		String line;
		String[] lineSplit;
		int result;
		while ((line = this.normalizedReader.readLine()) != null) {
			lineSplit = line.split("//s");
			for (String word : lineSplit) {
				result = BinarySearch.rank(word, this.wordIndex);
				assertTrue(result >= 0);
				assertTrue(result < this.wordIndex.length);
			}
		}
	}

	@Test
	public void testRankTwoParametersNormal() {
		String[] testArray = { "ab", "dr", "te", "yr" };
		assertEquals(0, BinarySearch.rank("aba", testArray));
		assertEquals(0, BinarySearch.rank("abz", testArray));
		assertEquals(0, BinarySearch.rank("abZ", testArray));
		assertEquals(0, BinarySearch.rank("dqz", testArray));
		assertEquals(0,
				BinarySearch.rank("dq" + Character.MAX_VALUE, testArray));
		assertEquals(1, BinarySearch.rank("ds", testArray));
		assertEquals(1, BinarySearch.rank("er", testArray));
		assertEquals(1, BinarySearch.rank("td", testArray));
		assertEquals(2, BinarySearch.rank("yqz", testArray));
		assertEquals(3, BinarySearch.rank("yr", testArray));
		assertEquals(3, BinarySearch.rank("yra", testArray));
		assertEquals(3, BinarySearch.rank("za", testArray));
		assertEquals(3, BinarySearch.rank(String.valueOf(Character.MAX_VALUE),
				testArray));
	}

	@Test
	public void testRankTwoParametersEdge() {
		String[] emptyArray = { "" };
		assertEquals(0, BinarySearch.rank("foo", emptyArray));
		assertEquals(0, BinarySearch.rank("", emptyArray));

		String[] testArray = { "ab", "dr", "te", "yr" };
		assertEquals(0, BinarySearch.rank("", testArray));

	}

	@Test(expected = NullPointerException.class)
	public void testRankTwoParametersFail1() {
		String[] nullArray = null;
		assertEquals(0, BinarySearch.rank("fail", nullArray));
	}

	@Test(expected = NullPointerException.class)
	public void testRankTwoParametersFail2() {
		String[] testArray = { "ab", "dr", "te", "yr" };
		assertEquals(0, BinarySearch.rank(null, testArray));
	}

}
