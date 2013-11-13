package de.typology.indexes;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class WordIndexerTest {
	File inputFile = new File("testDataset/training.txt");
	File indexFile = new File("testDataset/index.txt");

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		if (this.indexFile.exists()) {
			this.indexFile.delete();
		}
	}

	@After
	public void tearDown() throws Exception {
		if (this.indexFile.exists()) {
			this.indexFile.delete();
		}
	}

	@Test
	public void buildIndexTest() {
		WordIndexer wi = new WordIndexer();
		long maxCountPerFile = wi
				.buildIndex(this.inputFile, this.indexFile, 10);
		assertEquals(5, maxCountPerFile);
	}

}
