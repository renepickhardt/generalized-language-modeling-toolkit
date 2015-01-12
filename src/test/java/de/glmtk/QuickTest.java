package de.glmtk;

import java.nio.file.Paths;
import java.util.List;

import de.glmtk.common.BackoffMode;
import de.glmtk.common.CountCache;
import de.glmtk.common.NGram;
import de.glmtk.common.ProbMode;
import de.glmtk.querying.calculator.SequenceCalculator;
import de.glmtk.querying.estimator.Estimator;
import de.glmtk.querying.estimator.discount.ModKneserNeyDiscountEstimator;
import de.glmtk.querying.estimator.fraction.ContinuationMaximumLikelihoodEstimator;
import de.glmtk.querying.estimator.fraction.MaximumLikelihoodEstimator;
import de.glmtk.querying.estimator.interpol.DiffInterpolEstimator;
import de.glmtk.querying.estimator.interpol.InterpolEstimator;
import de.glmtk.testutil.LoggingTest;
import de.glmtk.util.StringUtils;

public class QuickTest extends LoggingTest {
    //    @Test
    //    public void test() throws Exception {
    public static void main(String args[]) throws Exception {
        LoggingTest.setUpLogging();
        //        ParamEstimator e = ParamEstimators.CMLE;
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
        //        ParamEstimator e = ParamEstimators.GLM;
        //        e.setCountCache(c.getCountCache());
        //        e.setProbMode(ProbMode.MARG);

        //        System.out.println(e.probability(new NGram("insolation"), new NGram(
        //                "_ _ _ local")));

        //        SequenceCalculator n = new SequenceCalculator();
        //        n.setEstimator(e);
        //        n.setProbMode(ProbMode.MARG);
        //
        //        String p = "the level of local insolation";
        //        double prob = n.probability(StringUtils.splitAtChar(p, ' '));
        //        System.out.println(p + "\t" + prob);

        //        Pattern a = Patterns.get("xxxx1x");
        //        System.out.println(a);
        //        System.out.println(a.getContinuationSource());
        //
        //        CountCache cc =
        //                new CountCache(
        //                        Paths.get("/home/lukas/langmodels/data/en0008t.out/testcounts/354440f49cc27ea0dd1a61fd719ef855"));
        //
        //        ParamEstimator mkn = ParamEstimators.GLM_ABS;
        //        mkn.setCountCache(cc);
        //        mkn.setProbMode(ProbMode.MARG);
        //
        //        List<Map.Entry<String, String>> seqs =
        //                new LinkedList<Map.Entry<String, String>>();
        //        try (BufferedReader reader =
        //                Files.newBufferedReader(
        //                        Paths.get("/home/lukas/langmodels/data/en0008t-t/5"),
        //                        Constants.CHARSET)) {
        //            String line;
        //            while ((line = reader.readLine()) != null) {
        //                int p = line.lastIndexOf(' ');
        //                String h = line.substring(0, p);
        //                String s = line.substring(p + 1);
        //                seqs.add(new AbstractMap.SimpleEntry<String, String>(s, h));
        //            }
        //        }
        //
        //        @SuppressWarnings("unchecked")
        //        AbstractMap.SimpleEntry<String, String>[] seqss =
        //        seqs.toArray(new AbstractMap.SimpleEntry[seqs.size()]);
        //
        //        double[] rels = new double[seqs.size()];
        //
        //        for (int j = 0; j != 3; ++j) {
        //
        //            long t = System.currentTimeMillis();
        //
        //            for (int i = 0; i != seqss.length; ++i) {
        //                Map.Entry<String, String> e = seqss[i];
        //                rels[i] =
        //                        mkn.probability(new NGram(e.getKey()), new NGram(
        //                                StringUtils.splitAtChar(e.getValue(), ' ')));
        //            }
        //
        //            t = System.currentTimeMillis() - t;
        //
        //            System.out.println(t + "ms");
        //        }
        //
        //        for (int i = 0; i != seqss.length; ++i) {
        //            System.out.println(seqss[i].getValue() + " " + seqss[i].getKey()
        //                    + " = " + rels[i]);
        //        }

        String[] ns = new String[6];
        Estimator[] es = new Estimator[6];

        ns[0] = "MKN_DEL";
        es[0] = new InterpolEstimator(
                new ModKneserNeyDiscountEstimator(
                        new MaximumLikelihoodEstimator()),
                new InterpolEstimator(
                        new ModKneserNeyDiscountEstimator(
                                new ContinuationMaximumLikelihoodEstimator()),
                        BackoffMode.DEL), BackoffMode.DEL);

        ns[1] = "MKN_SKP";
        es[1] = new InterpolEstimator(
                new ModKneserNeyDiscountEstimator(
                        new MaximumLikelihoodEstimator()),
                new InterpolEstimator(
                        new ModKneserNeyDiscountEstimator(
                                new ContinuationMaximumLikelihoodEstimator()),
                        BackoffMode.SKP), BackoffMode.SKP);

        ns[2] = "GLM_SKP";
        es[2] = new DiffInterpolEstimator(
                new ModKneserNeyDiscountEstimator(
                        new MaximumLikelihoodEstimator()),
                new DiffInterpolEstimator(
                        new ModKneserNeyDiscountEstimator(
                                new ContinuationMaximumLikelihoodEstimator()),
                        BackoffMode.SKP), BackoffMode.SKP);

        ns[3] = "GLM_DEL";
        es[3] = new DiffInterpolEstimator(
                new ModKneserNeyDiscountEstimator(
                        new MaximumLikelihoodEstimator()),
                new DiffInterpolEstimator(
                        new ModKneserNeyDiscountEstimator(
                                new ContinuationMaximumLikelihoodEstimator()),
                        BackoffMode.DEL), BackoffMode.DEL);

        ns[4] = "GLM_DEL_FRONT";
        es[4] = new DiffInterpolEstimator(
                new ModKneserNeyDiscountEstimator(
                        new MaximumLikelihoodEstimator()),
                new DiffInterpolEstimator(
                        new ModKneserNeyDiscountEstimator(
                                new ContinuationMaximumLikelihoodEstimator()),
                        BackoffMode.DEL_FRONT), BackoffMode.DEL_FRONT);

        ns[5] = "GLM_SKP_AND_DEL";
        es[5] = new DiffInterpolEstimator(
                new ModKneserNeyDiscountEstimator(
                        new MaximumLikelihoodEstimator()),
                new DiffInterpolEstimator(
                        new ModKneserNeyDiscountEstimator(
                                new ContinuationMaximumLikelihoodEstimator()),
                        BackoffMode.SKP_AND_DEL), BackoffMode.SKP_AND_DEL);

        CountCache cc = new CountCache(
                new GlmtkPaths(
                        Paths.get("/home/lukas/langmodels/data/en0008t.out/testcounts/354440f49cc27ea0dd1a61fd719ef855")));

        for (int i = 0; i != es.length; ++i) {
            String n = ns[i];
            System.out.println("# " + n);

            Estimator e = es[i];

            e.setCountCache(cc);
            e.setProbMode(ProbMode.MARG);

            SequenceCalculator c = new SequenceCalculator();
            c.setEstimator(e);
            c.setProbMode(ProbMode.MARG);

            List<String> w = StringUtils.splitAtChar(
                    "further edits should be made", ' ');
            NGram h = new NGram(w.subList(0, 4));
            NGram s = new NGram(w.get(4));

            double pp = c.probability(w);
            double p = e.probability(s, h);

            System.out.println(StringUtils.join(w, " ") + " = " + pp);
            System.out.println(s + "|" + h + " = " + p);
        }
    }
}
