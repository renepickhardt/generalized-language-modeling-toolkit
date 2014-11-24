package de.glmtk.querying.helper;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.BeforeClass;

public class TestCorporaTest extends LoggingTest {

    private static Logger LOGGER = LogManager.getLogger(TestCorporaTest.class);

    @BeforeClass
    public static void loadTestCorpora() throws IOException {
        LOGGER.info("Loading corpora...");
        // Those calls trigger the static variables to be initialized
        // so their logging output appears before any test output.
        TestCorpus.ABC.getCorpusName();
        TestCorpus.MOBY_DICK.getCorpusName();
        TestCorpus.EN0008T.getCorpusName();
    }

}
