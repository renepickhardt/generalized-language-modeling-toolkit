package de.glmtk.testutil;

import org.junit.BeforeClass;

public class TestCorporaTest extends ConfigTest {
    @BeforeClass
    public static void setUpTestCoprpora() throws Exception {
        TestCorpus.intializeTestCorpora(config);
    }
}
