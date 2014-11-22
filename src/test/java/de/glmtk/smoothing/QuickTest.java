package de.glmtk.smoothing;

import java.io.IOException;

import org.junit.Test;

import de.glmtk.smoothing.helper.LoggingTest;

public class QuickTest extends LoggingTest {

    @Test
    public void test() throws IOException {
        System.out.println("a" + 'b');
        //        Estimator e = Estimators.CMLE;
        //        e.setCountCache(TestCorpus.ABC.getCountCache());
        //        e.setProbMode(ProbMode.COND);
        //        NGram history = new NGram(Arrays.asList("b", "a", "a"));
        //        NGram sequence = new NGram("b");
        //        System.out.println(history.seen(TestCorpus.ABC.getCountCache()));
        //        System.out.println(e.probability(sequence, history));
        //
        //        String a = "bbad asoim an *! . \\, adv. $*";
        //        System.out.println(a);
        //        System.out.println(a.replaceAll("[^\\w ]", "\\\\$0"));
    }

}
