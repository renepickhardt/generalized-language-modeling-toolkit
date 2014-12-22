package de.glmtk.experiments;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.glmtk.utils.StringUtils;

/**
 * 2014-12-21
 * Experiment to get a feel about GLM recursiveness and eventually get a formula
 * to how many unique skipped sequences per order there are.
 *
 * <p>
 * Result: The number of unique skipped sequences per order is:
 * (n!)/((n-order)!*order!). See <a href=
 * "https://en.wikipedia.org/wiki/Multinomial_theorem#Multinomial_coefficients"
 * >Multinomial coefficient</a>.
 *
 * <p>
 * Uses Ansi-Color-Codes to color recurring calls. If you are using Eclipse and
 * want it to see colors, install <a
 * href="http://mihai-nita.net/2013/06/03/eclipse-plugin-ansi-in-console/">
 * AnsiConsole</a>.
 */
public class E01_GlmCountSkpSeqs {

    private static String colorStart(int color) {
        return "\u001b[1;3" + color + "m";
    }

    private static String colorEnd() {
        return "\u001b[0m";
    }

    private static List<String> skpSequence(List<String> sequence, int skpPos) {
        List<String> result = new ArrayList<String>(sequence);
        for (int i = 0; i != result.size(); ++i) {
            if (i == skpPos) {
                result.set(i, "*");
            }
        }
        return result;
    }

    public static void main(String args[]) {
        List<String> sequence = Arrays.asList("a", "b", "c", "d", "e");

        List<List<List<String>>> ss = new ArrayList<List<List<String>>>();
        ss.add(Arrays.asList(sequence));
        List<List<Integer>> cc = new ArrayList<List<Integer>>();
        cc.add(Arrays.asList(0));

        for (int order = 1; order != sequence.size() + 1; ++order) {
            List<List<String>> s = new ArrayList<List<String>>();
            ss.add(s);
            List<Integer> c = new ArrayList<Integer>();
            cc.add(c);

            List<List<String>> seqs = ss.get(order - 1);
            List<Integer> cols = cc.get(order - 1);
            for (int i = 0; i != seqs.size(); ++i) {
                List<String> seq = seqs.get(i);
                int col = cols.get(i);

                for (int j = 0; j != seq.size(); ++j) {
                    List<String> skpSeq = skpSequence(seq, j);
                    if (!skpSeq.equals(seq)) {
                        if (col == 0) {
                            if (s.contains(skpSeq)) {
                                c.add(1);
                            } else {
                                c.add(0);
                            }
                        } else {
                            c.add(col + 1);
                        }
                        s.add(skpSeq);
                    }
                }
            }
        }

        for (int order = 0; order != sequence.size() + 1; ++order) {
            List<List<String>> s = ss.get(order);
            List<Integer> c = cc.get(order);

            System.out.println("-----------");
            System.out.println("--- order=" + order);
            System.out.println("--- count=" + s.size());
            for (int i = 0; i != order; ++i) {
                int cnt = 0;
                for (int col : c) {
                    if (col == i) {
                        ++cnt;
                    }
                }
                System.out.println("--- col-" + i + "=" + cnt);
            }
            for (int i = 0; i != s.size(); ++i) {
                List<String> seq = s.get(i);
                int col = c.get(i);
                System.out.println(colorStart(col) + StringUtils.join(seq, " ")
                        + colorEnd());
            }
        }
        System.out.println("-----------");

        System.out.println("\nUniques:");
        for (int order = 0; order != sequence.size() + 1; ++order) {
            List<List<String>> s = ss.get(order);
            List<Integer> c = cc.get(order);

            System.out.println("-----------");
            System.out.println("--- order=" + order);
            int cnt = 0;
            for (int col : c) {
                if (col == 0) {
                    ++cnt;
                }
            }
            System.out.println("--- count=" + cnt);
            for (int i = 0; i != s.size(); ++i) {
                List<String> seq = s.get(i);
                int col = c.get(i);
                if (col == 0) {
                    System.out.println(colorStart(col)
                            + StringUtils.join(seq, " ") + colorEnd());
                }
            }
        }
        System.out.println("-----------");
    }
}
