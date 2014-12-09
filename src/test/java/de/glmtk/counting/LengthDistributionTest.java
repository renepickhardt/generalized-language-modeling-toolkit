package de.glmtk.counting;

import static de.glmtk.Constants.TEST_RESSOURCES_DIR;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Assert;
import org.junit.Test;

// TODO: Make a not rudimentary test.

public class LengthDistributionTest {

    @Test
    public void test() throws IOException {
        LengthDistribution d =
                new LengthDistribution(TEST_RESSOURCES_DIR.resolve("en0008t"),
                        true);
        Path s = Files.createTempFile("", "");
        d.writeToStore(s);

        LengthDistribution e = new LengthDistribution(s, false);

        for (int i = 0; i != d.getMaxLength() + 1; ++i) {
            double df = d.getLengthFrequency(i);
            double ef = e.getLengthFrequency(i);
            Assert.assertEquals(df, ef, 0.01);
        }
    }

}
