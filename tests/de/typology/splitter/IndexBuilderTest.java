package de.typology.splitter;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.typology.testutils.Resetter;
import de.typology.utils.Config;

public class IndexBuilderTest {
	private String outputDirectory;
	IndexBuilder ib;
	// private BufferedReader normalizedReader;
	private String[] wordIndex;

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
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		// remove the following part to keep the generated output
		Resetter.reset("testDataset/");
	}

	@Before
	public void setUp() throws Exception {
		this.outputDirectory = "testDataset/";
		this.ib = new IndexBuilder();
		this.wordIndex = this.ib.deserializeIndex(this.outputDirectory
				+ "index.txt");
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testIndexSize() {
		assertTrue(this.wordIndex.length > 0);
		// the length of wordIndex can be bigger than maxCountDivider since
		// files can be smaller than maxCount
		// assertTrue(this.wordIndex.length <= Config.get().maxCountDivider);
	}

}
