package de.typology.splitter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.typology.indexes.WordIndex;

public class SequencerTest {
	File inputFile = new File("testDataset/training.txt");
	File indexFile = new File("testDataset/index.txt");
	File sequencerOutputDirectory = new File("testDataset/sequencer/");

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		if (this.sequencerOutputDirectory.exists()) {
			FileUtils.deleteDirectory(this.sequencerOutputDirectory);
		}
		this.sequencerOutputDirectory.mkdir();
	}

	@After
	public void tearDown() throws Exception {
		if (this.sequencerOutputDirectory.exists()) {
			FileUtils.deleteDirectory(this.sequencerOutputDirectory);
		}
	}

	@Test
	public void squencing1Test() {
		WordIndex wordIndex = new WordIndex(this.indexFile);
		boolean[] pattern = { true };

		try {
			InputStream inputStream = new FileInputStream(this.inputFile);
			Sequencer sequencer = new Sequencer(inputStream,
					this.sequencerOutputDirectory, wordIndex, pattern);
			sequencer.run();

			// test file contents
			BufferedReader br0 = new BufferedReader(new FileReader(
					this.sequencerOutputDirectory.getAbsolutePath() + "/3"));
			for (int i = 0; i < 6; i++) {
				assertEquals("dolor", br0.readLine());
			}
			assertNull(br0.readLine());
			br0.close();

			BufferedReader br10 = new BufferedReader(new FileReader(
					this.sequencerOutputDirectory.getAbsolutePath() + "/10"));
			assertEquals("tempor", br10.readLine());
			assertEquals("ut", br10.readLine());
			assertEquals("voluptua.", br10.readLine());
			assertNull(br10.readLine());
			br10.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void squencing1101Test() {
		WordIndex wordIndex = new WordIndex(this.indexFile);
		boolean[] pattern = { true, true, false, true };

		try {
			InputStream inputStream = new FileInputStream(this.inputFile);
			Sequencer sequencer = new Sequencer(inputStream,
					this.sequencerOutputDirectory, wordIndex, pattern);
			sequencer.run();

			// test file contents
			BufferedReader br0 = new BufferedReader(new FileReader(
					this.sequencerOutputDirectory.getAbsolutePath() + "/3"));
			for (int i = 0; i < 3; i++) {
				assertEquals("dolor sit consetetur", br0.readLine());
			}
			assertNull(br0.readLine());
			br0.close();

			BufferedReader br10 = new BufferedReader(new FileReader(
					this.sequencerOutputDirectory.getAbsolutePath() + "/10"));
			assertEquals("tempor invidunt labore", br10.readLine());
			assertEquals("ut labore dolore", br10.readLine());
			assertNull(br10.readLine());
			br10.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
