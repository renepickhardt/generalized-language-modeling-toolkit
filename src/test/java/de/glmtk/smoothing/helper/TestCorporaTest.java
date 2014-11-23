package de.glmtk.smoothing.helper;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.BeforeClass;

public class TestCorporaTest extends LoggingTest {

    private static Logger LOGGER = LogManager.getLogger(TestCorporaTest.class);

    @BeforeClass
    public static void loadTestCorpora() throws IOException {
        LOGGER.info("Loading corpora...");
        TestCorpus.ABC.getCountCache();
        TestCorpus.MOBY_DICK.getCountCache();
        TestCorpus.EN0008T.getCountCache();
    }

}
