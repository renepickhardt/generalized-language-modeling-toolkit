package de.glmtk.smoothing;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Test;

import de.glmtk.smoothing.estimator.Estimator;
import de.glmtk.smoothing.estimator.Estimators;
import de.glmtk.smoothing.helper.LoggingTest;
import de.glmtk.smoothing.helper.TestCorpus;

public class QuickTest extends LoggingTest {

    @Test
    public void test() throws IOException {
        Estimator e = Estimators.CMLE;
        e.setCountCache(TestCorpus.ABC.getCountCache());
        e.setProbMode(ProbMode.COND);
        NGram history = new NGram(Arrays.asList("b", "a", "a"));
        NGram sequence = new NGram("b");
        System.out.println(history.seen(TestCorpus.ABC.getCountCache()));
        System.out.println(e.probability(sequence, history));

        String a = "bbad asoim an *! . \\, adv. $*";
        System.out.println(a);
        System.out.println(a.replaceAll("[^\\w ]", "\\\\$0"));
    }

}
