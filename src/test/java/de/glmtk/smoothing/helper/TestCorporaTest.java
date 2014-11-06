package de.glmtk.smoothing.helper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.BeforeClass;

public class TestCorporaTest extends LoggingTest {

    private static Logger LOGGER = LogManager.getLogger(TestCorporaTest.class);

    @BeforeClass
    public static void loadTestCorpora() {
        LOGGER.info("Loading corpora...");
        TestCorpus.ABC.getCorpusName();
        TestCorpus.MOBY_DICK.getCorpusName();
        TestCorpus.EN0008T.getCorpusName();
    }

}
