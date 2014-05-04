package de.typology.indexing;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.typology.indexing.WordIndexBuilder;

public class WordIndexerTest {

    File trainingFile = new File("testDataset/training.txt");

    File indexFile = new File("testDataset/index.txt");

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        if (indexFile.exists()) {
            indexFile.delete();
        }
    }

    @After
    public void tearDown() throws Exception {
        if (indexFile.exists()) {
            indexFile.delete();
        }
    }

    @Test
    public void buildIndexTest() throws IOException {
        WordIndexBuilder wi = new WordIndexBuilder();
        wi.buildIndex(Files.newInputStream(trainingFile.toPath()),
                Files.newOutputStream(indexFile.toPath()), 10, "<fs> <s> ",
                " </s>");
    }
}
