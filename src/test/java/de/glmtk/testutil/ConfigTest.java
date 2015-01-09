package de.glmtk.testutil;

import static de.glmtk.common.Output.OUTPUT;

import java.io.IOException;

import org.junit.BeforeClass;

import de.glmtk.common.Config;

public class ConfigTest extends LoggingTest {
    protected static Config config;

    @BeforeClass
    public static void setUpConfig() throws IOException {
        config = new Config();
        OUTPUT.initialize(config);
    }
}
