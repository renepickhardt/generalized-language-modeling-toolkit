package de.typology.splitter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SequenceModifierTest {
	File inputDirectory = new File("testDataset/sequenceModifier");
	OutputStream outputStream;
	private PipedInputStream pipedInputStream;
	private OutputStream pipedOutputStream;

	@Before
	public void setUp() throws Exception {
		if (this.inputDirectory.exists()) {
			FileUtils.deleteDirectory(this.inputDirectory);
		}
		this.inputDirectory.mkdir();
		BufferedWriter br1 = new BufferedWriter(new FileWriter(
				this.inputDirectory.getAbsolutePath() + "/1"));
		br1.write("a b c\t13\n");
		br1.write("d e f\t14\n");
		br1.write("g h i\t15\n");
		br1.close();
		BufferedWriter br2 = new BufferedWriter(new FileWriter(
				this.inputDirectory.getAbsolutePath() + "/2"));
		br2.write("j k l\t16\n");
		br2.write("m n o\t17\n");
		br2.write("ä ö ü\t18\n");
		br2.write("p q r\t19\n");
		br2.close();
		this.pipedInputStream = new PipedInputStream(10 * 8 * 1024);
		this.pipedOutputStream = new PipedOutputStream(this.pipedInputStream);
	}

	@After
	public void tearDown() throws Exception {
		if (this.inputDirectory.exists()) {
			FileUtils.deleteDirectory(this.inputDirectory);
		}
	}

	@Test
	public void sequenceModifier101Test() {
		boolean[] pattern = { true, false, true };

		SequenceModifier sequenceModifier = new SequenceModifier(
				this.inputDirectory, this.pipedOutputStream, "\n", pattern);
		sequenceModifier.run();
		BufferedReader bufferedReader = new BufferedReader(
				new InputStreamReader(this.pipedInputStream));

		try {
			assertEquals("a c", bufferedReader.readLine());
			assertEquals("d f", bufferedReader.readLine());
			assertEquals("g i", bufferedReader.readLine());
			assertEquals("j l", bufferedReader.readLine());
			assertEquals("m o", bufferedReader.readLine());
			assertEquals("ä ü", bufferedReader.readLine());
			assertEquals("p r", bufferedReader.readLine());
			assertNull(bufferedReader.readLine());
			bufferedReader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
