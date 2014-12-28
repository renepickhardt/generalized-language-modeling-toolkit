package de.glmtk.testutils;

import static de.glmtk.utils.LogUtils.LOG_UTILS;

import org.junit.BeforeClass;

public class LoggingTest {

    @BeforeClass
    public static void setUpLogging() {
        LOG_UTILS.setUpTestLogging();
    }

}
