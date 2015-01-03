package de.glmtk.testutil;

import static de.glmtk.util.LoggingHelper.LOGGING_HELPER;

import org.apache.logging.log4j.core.appender.ConsoleAppender.Target;
import org.junit.BeforeClass;

public class LoggingTest {
    @BeforeClass
    public static void setUpLogging() {
        LOGGING_HELPER.addConsoleAppender(Target.SYSTEM_OUT);
    }
}
