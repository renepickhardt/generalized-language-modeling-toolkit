package de.typology.splitter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

public class AggregatorTest {

    File trainingFile = new File("testDataset/aggregator-in.txt");

    File outputFile = new File("testDataset/aggregator-out.txt");

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        BufferedWriter br = new BufferedWriter(new FileWriter(trainingFile));
        br.write("b y b\t1\n");
        br.write("c x a\t1\n");
        br.write("b y a\t1\n");
        br.write("a z a\t1\n");
        br.write("c y b\t1\n");
        br.write("c x a\t1\n");
        br.close();
    }

    @After
    public void tearDown() throws Exception {
        trainingFile.delete();
    }

    @Test
    public void aggregatorCol0Test() throws IOException {
        InputStream input = new FileInputStream(trainingFile);
        OutputStream output = new FileOutputStream(outputFile);
        Aggregator aggregator = new Aggregator(input, output, "\t", false);
        aggregator.aggregateCounts();
        try {
            BufferedReader br = new BufferedReader(new FileReader(outputFile));
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
        outputFile.delete();

    }

    //    @Test
    //    public void aggregatorCol1Test() throws IOException {
    //        Aggregator aggregator =
    //                new Aggregator(trainingFile, outputFile, "\t", 1, false);
    //        aggregator.aggregateCounts();
    //        try {
    //            BufferedReader br = new BufferedReader(new FileReader(outputFile));
    //            assertEquals("c x a\t2", br.readLine());
    //            assertEquals("b y a\t1", br.readLine());
    //            assertEquals("b y b\t1", br.readLine());
    //            assertEquals("c y b\t1", br.readLine());
    //            assertEquals("a z a\t1", br.readLine());
    //            assertNull(br.readLine());
    //            br.close();
    //        } catch (IOException e) {
    //            // TODO Auto-generated catch block
    //            e.printStackTrace();
    //        }
    //        outputFile.delete();
    //    }
    //
    //    @Test
    //    public void aggregatorCol2Test() throws IOException {
    //        Aggregator aggregator =
    //                new Aggregator(trainingFile, outputFile, "\t", 2, false);
    //        aggregator.aggregateCounts();
    //        try {
    //            BufferedReader br = new BufferedReader(new FileReader(outputFile));
    //            assertEquals("a z a\t1", br.readLine());
    //            assertEquals("b y a\t1", br.readLine());
    //            assertEquals("c x a\t2", br.readLine());
    //            assertEquals("b y b\t1", br.readLine());
    //            assertEquals("c y b\t1", br.readLine());
    //            assertNull(br.readLine());
    //            br.close();
    //        } catch (IOException e) {
    //            // TODO Auto-generated catch block
    //            e.printStackTrace();
    //        }
    //        outputFile.delete();
    //    }
}
