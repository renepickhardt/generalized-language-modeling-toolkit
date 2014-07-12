package de.typology.smoothing;

import org.junit.BeforeClass;

import de.typology.Logging;

public class LoggingTest {

    @BeforeClass
    public static void setUpLogging() {
        Logging.configureTestLogging();
    }

}
