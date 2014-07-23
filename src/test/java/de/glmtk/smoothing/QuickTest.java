package de.glmtk.smoothing;

import java.io.IOException;
import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.Test;

import de.glmtk.smoothing.calculating.SequenceCalculator;
import de.glmtk.smoothing.calculating.SkipCalculator;
import de.glmtk.smoothing.estimating.Estimator;
import de.glmtk.smoothing.estimating.Estimators;
import de.glmtk.smoothing.helper.AbcTestCorpus;
import de.glmtk.smoothing.helper.LoggingTest;
import de.glmtk.smoothing.helper.MobyDickTestCorpus;
import de.glmtk.smoothing.helper.TestCorpus;

public class QuickTest extends LoggingTest {

    protected static TestCorpus abcTestCorpus;

    protected static Corpus abcCorpus;

    protected static TestCorpus mobyDickTestCorpus;

    protected static Corpus mobyDickCorpus;

    @BeforeClass
    public static void setUpCorpora() throws IOException, InterruptedException {
        abcTestCorpus = new AbcTestCorpus();
        abcCorpus = abcTestCorpus.getCorpus();
        mobyDickTestCorpus = new MobyDickTestCorpus();
        mobyDickCorpus = mobyDickTestCorpus.getCorpus();
    }

    @Test
    public void test() {
        Estimator e = Estimators.INTERPOL_ABS_DISCOUNT_MLE;
        SequenceCalculator c = new SkipCalculator();
        e.setCorpus(abcCorpus);
        System.out.println(c.propability(e,
                Arrays.asList("a", "a", "a", "a", "a")));
    }

}
