package de.glmtk.smoothing;

import java.io.IOException;
import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.Test;

import de.glmtk.smoothing.estimator.Estimator;
import de.glmtk.smoothing.estimator.Estimators;
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
        Estimator e = Estimators.CMLE;
        e.setCorpus(abcCorpus);
        e.setProbMode(ProbMode.COND);
        NGram history = new NGram(Arrays.asList("b", "a", "a"));
        NGram sequence = new NGram("b");
        System.out.println(history.seen(abcCorpus));
        System.out.println(e.probability(sequence, history));
    }

}
