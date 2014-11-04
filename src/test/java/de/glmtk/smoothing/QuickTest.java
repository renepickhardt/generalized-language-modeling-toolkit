package de.glmtk.smoothing;

import java.util.Arrays;

import org.junit.Test;

import de.glmtk.smoothing.estimator.Estimator;
import de.glmtk.smoothing.estimator.Estimators;
import de.glmtk.smoothing.helper.LoggingTest;
import de.glmtk.smoothing.helper.TestCorpus;

public class QuickTest extends LoggingTest {

    @Test
    public void test() {
        Estimator e = Estimators.CMLE;
        e.setCorpus(TestCorpus.ABC.getCorpus());
        e.setProbMode(ProbMode.COND);
        NGram history = new NGram(Arrays.asList("b", "a", "a"));
        NGram sequence = new NGram("b");
        System.out.println(history.seen(TestCorpus.ABC.getCorpus()));
        System.out.println(e.probability(sequence, history));
    }

}
