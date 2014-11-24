package de.glmtk.querying;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;

import org.junit.Test;

import de.glmtk.querying.helper.TestCorporaTest;
import de.glmtk.querying.helper.TestCorpus;
import de.glmtk.utils.CountCache;
import de.glmtk.utils.NGram;
import de.glmtk.utils.Pattern;

public class QuickTest extends TestCorporaTest {

    @Test
    public void test() throws IOException, IllegalArgumentException,
            IllegalAccessException, NoSuchFieldException, SecurityException {
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

        NGram a = new NGram("a");

        CountCache countCache = TestCorpus.ABC.getCountCache();
        System.out.println(countCache.getNumWords());
        System.out.println(countCache.getAbsolute(a));

        Field absoluteField = CountCache.class.getDeclaredField("absolute");
        absoluteField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Pattern, Map<String, Long>> absolute =
        (Map<Pattern, Map<String, Long>>) absoluteField.get(countCache);
        System.out.println(absolute.get(a.getPattern()).get(a.toString()));
    }
}
