package de.glmtk.querying.helper;

import org.junit.BeforeClass;

import de.glmtk.utils.LogUtils;

public class LoggingTest {

    @BeforeClass
    public static void setUpLogging() {
        LogUtils.setUpTestLogging();
    }

}
