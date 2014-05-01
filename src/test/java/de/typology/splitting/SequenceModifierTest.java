package de.typology.splitting;

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

import de.typology.counting.SequenceModifierTask;

public class SequenceModifierTest {

    File workingDirectory = new File("testDataset/sequenceModifier");

    OutputStream outputStream;

    private PipedInputStream pipedInputStream;

    private OutputStream pipedOutputStream;

    @Before
    public void setUp() throws Exception {
        if (workingDirectory.exists()) {
            FileUtils.deleteDirectory(workingDirectory);
        }
        workingDirectory.mkdir();
        BufferedWriter br1 =
                new BufferedWriter(new FileWriter(
                        workingDirectory.getAbsolutePath() + "/1"));
        br1.write("a b c\t13\n");
        br1.write("d e f\t14\n");
        br1.write("g h i\t15\n");
        br1.close();
        BufferedWriter br2 =
                new BufferedWriter(new FileWriter(
                        workingDirectory.getAbsolutePath() + "/2"));
        br2.write("j k l\t16\n");
        br2.write("m n o\t17\n");
        br2.write("ä ö ü\t18\n");
        br2.write("p q r\t19\n");
        br2.close();
        pipedInputStream = new PipedInputStream(10 * 8 * 1024);
        pipedOutputStream = new PipedOutputStream(pipedInputStream);
    }

    @After
    public void tearDown() throws Exception {
        if (workingDirectory.exists()) {
            FileUtils.deleteDirectory(workingDirectory);
        }
    }

    @Test
    public void sequenceModifier101Test() {
        boolean[] pattern = {
            true, false, true
        };

        SequenceModifierTask sequenceModifier =
                new SequenceModifierTask(workingDirectory.toPath(),
                        pipedOutputStream, "\t", pattern, true);
        sequenceModifier.run();
        BufferedReader bufferedReader =
                new BufferedReader(new InputStreamReader(pipedInputStream));

        try {
            assertEquals("a c\t1", bufferedReader.readLine());
            assertEquals("d f\t1", bufferedReader.readLine());
            assertEquals("g i\t1", bufferedReader.readLine());
            assertEquals("j l\t1", bufferedReader.readLine());
            assertEquals("m o\t1", bufferedReader.readLine());
            assertEquals("ä ü\t1", bufferedReader.readLine());
            assertEquals("p r\t1", bufferedReader.readLine());
            assertNull(bufferedReader.readLine());
            bufferedReader.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
