package de.typology.indexing;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.typology.indexing.WordIndex;
import de.typology.indexing.WordIndexer;

public class WordIndexTest {

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
        WordIndexer wi = new WordIndexer();
        wi.buildIndex(Files.newInputStream(trainingFile.toPath()),
                Files.newOutputStream(indexFile.toPath()), 10, "<fs> <s> ",
                " </s>");
    }

    @After
    public void tearDown() throws Exception {
        if (indexFile.exists()) {
            indexFile.delete(); 
        } 
    }

    @Test
    public void rankTest() throws IOException {
        WordIndex wi = new WordIndex(new FileInputStream(indexFile));
        assertEquals(8, wi.rank("et"));
        assertEquals(3, wi.rank("A"));
        assertEquals(4, wi.rank("Z"));
        assertEquals(11, wi.rank("tempora"));
        assertEquals(11, wi.rank("z"));
    }

}
