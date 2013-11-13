package de.typology.splitter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

public class AggregatorTest {
	File inputFile = new File("testDataset/aggregator-in.txt");
	File outputFile = new File("testDataset/aggregator-out.txt");

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		BufferedWriter br = new BufferedWriter(new FileWriter(this.inputFile));
		br.write("b y b\n");
		br.write("c x a\n");
		br.write("b y a\n");
		br.write("a z a\n");
		br.write("c y b\n");
		br.write("c x a\n");
		br.close();
	}

	@After
	public void tearDown() throws Exception {
		this.inputFile.delete();
	}

	@Test
	public void aggregatorCol0Test() {
		Aggregator aggregator = new Aggregator(this.inputFile, this.outputFile,
				"\t", 0);
		aggregator.run();
		try {
			BufferedReader br = new BufferedReader(new FileReader(
					this.outputFile));
			assertEquals("a z a\t1", br.readLine());
			assertEquals("b y a\t1", br.readLine());
			assertEquals("b y b\t1", br.readLine());
			assertEquals("c x a\t2", br.readLine());
			assertEquals("c y b\t1", br.readLine());
			assertNull(br.readLine());
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.outputFile.delete();

	}

	@Test
	public void aggregatorCol1Test() {
		Aggregator aggregator = new Aggregator(this.inputFile, this.outputFile,
				"\t", 1);
		aggregator.run();
		try {
			BufferedReader br = new BufferedReader(new FileReader(
					this.outputFile));
			assertEquals("c x a\t2", br.readLine());
			assertEquals("b y a\t1", br.readLine());
			assertEquals("b y b\t1", br.readLine());
			assertEquals("c y b\t1", br.readLine());
			assertEquals("a z a\t1", br.readLine());
			assertNull(br.readLine());
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.outputFile.delete();
	}
}
