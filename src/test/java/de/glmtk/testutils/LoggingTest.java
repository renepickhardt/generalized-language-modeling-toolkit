package de.glmtk.testutils;

import org.junit.BeforeClass;

import de.glmtk.utils.LogUtils;

public class LoggingTest {

    @BeforeClass
    public static void setUpLogging() {
        LogUtils.getInstance().setUpTestLogging();
    }

}
