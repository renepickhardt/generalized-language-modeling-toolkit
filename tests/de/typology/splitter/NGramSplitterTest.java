/**
 * 
 */
package de.typology.splitter;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.typology.testutils.Resetter;

/**
 * @author mkoerner
 * 
 */
public class NGramSplitterTest {

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		// reset testDataset/
		Resetter.reset("testDataset/");

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
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		System.out.println("test passed");
	}

}
