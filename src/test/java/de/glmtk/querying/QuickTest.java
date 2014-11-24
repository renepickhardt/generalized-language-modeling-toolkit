package de.glmtk.querying;

import de.glmtk.querying.estimator.Estimator;
import de.glmtk.querying.estimator.Estimators;
import de.glmtk.querying.helper.TestCorporaTest;
import de.glmtk.querying.helper.TestCorpus;
import de.glmtk.utils.LogUtils;
import de.glmtk.utils.StringUtils;

public class QuickTest extends TestCorporaTest {

    //    @Test
    //    public void test() throws Exception {
    public static void main(String args[]) throws Exception {
        LogUtils.setUpTestLogging();
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

        //        NGram a = new NGram("a");
        //
        //        CountCache countCache = TestCorpus.ABC.getCountCache();
        //        System.out.println(countCache.getNumWords());
        //        System.out.println(countCache.getAbsolute(a));
        //
        //        Field absoluteField = CountCache.class.getDeclaredField("absolute");
        //        absoluteField.setAccessible(true);
        //        @SuppressWarnings("unchecked")
        //        Map<Pattern, Map<String, Long>> absolute =
        //        (Map<Pattern, Map<String, Long>>) absoluteField.get(countCache);
        //        System.out.println(absolute.get(a.getPattern()).get(a.toString()));

        TestCorpus c = TestCorpus.EN0008T;

        Estimator e = Estimators.GLM;
        e.setCountCache(c.getCountCache());
        e.setProbMode(ProbMode.MARG);

        //        System.out.println(e.probability(new NGram("insolation"), new NGram(
        //                "_ _ _ local")));

        NGramProbabilityCalculator n = new NGramProbabilityCalculator();
        n.setEstimator(e);
        n.setProbMode(ProbMode.MARG);

        String p = "the level of local insolation";
        double prob = n.probability(StringUtils.splitAtChar(p, ' '));
        System.out.println(p + "\t" + prob);
    }
}
