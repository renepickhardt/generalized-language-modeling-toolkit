package de.glmtk.smoothing;

import org.junit.BeforeClass;

import de.glmtk.Logging;

public class LoggingTest {

    @BeforeClass
    public static void setUpLogging() {
        Logging.configureTestLogging();
    }

}
