package de.glmtk.querying;

import java.io.BufferedReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.glmtk.querying.estimator.Estimator;
import de.glmtk.querying.estimator.Estimators;
import de.glmtk.querying.helper.TestCorporaTest;
import de.glmtk.utils.CountCache;
import de.glmtk.utils.LogUtils;
import de.glmtk.utils.NGram;
import de.glmtk.utils.Pattern;
import de.glmtk.utils.Patterns;
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

        //        TestCorpus c = TestCorpus.EN0008T;
        //
        //        Estimator e = Estimators.GLM;
        //        e.setCountCache(c.getCountCache());
        //        e.setProbMode(ProbMode.MARG);

        //        System.out.println(e.probability(new NGram("insolation"), new NGram(
        //                "_ _ _ local")));

        //        NGramProbabilityCalculator n = new NGramProbabilityCalculator();
        //        n.setEstimator(e);
        //        n.setProbMode(ProbMode.MARG);
        //
        //        String p = "the level of local insolation";
        //        double prob = n.probability(StringUtils.splitAtChar(p, ' '));
        //        System.out.println(p + "\t" + prob);

        Pattern a = Patterns.get("xxxx1x");
        System.out.println(a);
        System.out.println(a.getContinuationSource());

        CountCache cc =
                new CountCache(
                        Paths.get("/home/lukas/langmodels/data/en0008t.out/testcounts/354440f49cc27ea0dd1a61fd719ef855"));

        Estimator mkn = Estimators.GLM_ABS;
        mkn.setCountCache(cc);
        mkn.setProbMode(ProbMode.MARG);

        List<Map.Entry<String, String>> seqs =
                new LinkedList<Map.Entry<String, String>>();
        try (BufferedReader reader =
                Files.newBufferedReader(
                        Paths.get("/home/lukas/langmodels/data/en0008t-t/5"),
                        Charset.defaultCharset())) {
            String line;
            while ((line = reader.readLine()) != null) {
                int p = line.lastIndexOf(' ');
                String h = line.substring(0, p);
                String s = line.substring(p + 1);
                seqs.add(new AbstractMap.SimpleEntry<String, String>(s, h));
            }
        }

        @SuppressWarnings("unchecked")
        AbstractMap.SimpleEntry<String, String>[] seqss =
        seqs.toArray(new AbstractMap.SimpleEntry[seqs.size()]);

        double[] rels = new double[seqs.size()];

        for (int j = 0; j != 3; ++j) {

            long t = System.currentTimeMillis();

            for (int i = 0; i != seqss.length; ++i) {
                Map.Entry<String, String> e = seqss[i];
                rels[i] =
                        mkn.probability(new NGram(e.getKey()), new NGram(
                                StringUtils.splitAtChar(e.getValue(), ' ')));
            }

            t = System.currentTimeMillis() - t;

            System.out.println(t + "ms");
        }

        for (int i = 0; i != seqss.length; ++i) {
            System.out.println(seqss[i].getValue() + " " + seqss[i].getKey()
                    + " = " + rels[i]);
        }

    }
}
