package de.glmtk.counting;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

import de.glmtk.testutils.TestCorporaTest;
import de.glmtk.testutils.TestCorpus;

// TODO: Make a not rudimentary test.

public class LengthDistributionTest extends TestCorporaTest {

    @Test
    public void testPersistency() throws IOException {
        LengthDistribution d =
                new LengthDistribution(TestCorpus.EN0008T.getCorpus(), true);
        Path s = Files.createTempFile("", "");
        d.writeToStore(s);

        LengthDistribution e = new LengthDistribution(s, false);

        for (int i = 1; i != d.getMaxLength() + 1; ++i) {
            double df = d.getLengthFrequency(i);
            double ef = e.getLengthFrequency(i);
            assertEquals(df, ef, 0.01);
        }
    }

    @Test
    public void testSumEqualsOne() throws IOException {
        LengthDistribution d =
                new LengthDistribution(TestCorpus.EN0008T.getCorpus(), true);

        double sum = 0;
        for (int i = 1; i != d.getMaxLength() + 1; ++i) {
            double f = d.getLengthFrequency(i);
            sum += f;
        }
        assertEquals(1.0, sum, 0.01);
    }

}
