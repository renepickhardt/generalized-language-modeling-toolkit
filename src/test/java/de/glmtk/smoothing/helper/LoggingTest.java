package de.glmtk.smoothing.helper;

import org.junit.BeforeClass;

import de.glmtk.utils.Logging;

public class LoggingTest {

    @BeforeClass
    public static void setUpLogging() {
        Logging.configureTestLogging();
    }

}
