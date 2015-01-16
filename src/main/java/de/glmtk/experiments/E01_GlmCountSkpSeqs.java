/*
 * Generalized Language Modeling Toolkit (GLMTK)
 *
 * Copyright (C) 2014-2015 Lukas Schmelzeisen
 *
 * GLMTK is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * GLMTK is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * GLMTK. If not, see <http://www.gnu.org/licenses/>.
 *
 * See the AUTHORS file for contributors.
 */

package de.glmtk.experiments;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.glmtk.util.StringUtils;

/**
 * 2014-12-21 Experiment to get a feel about GLM recursiveness and eventually
 * get a formula to how many unique skipped sequences per order there are.
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

    private static List<String> skpSequence(List<String> sequence,
            int skpPos) {
        List<String> result = new ArrayList<>(sequence);
        for (int i = 0; i != result.size(); ++i)
            if (i == skpPos)
                result.set(i, "*");
        return result;
    }

    public static void main(String args[]) {
        List<String> sequence = Arrays.asList("a", "b", "c", "d", "e");

        List<List<List<String>>> ss = new ArrayList<>();
        ss.add(Arrays.asList(sequence));
        List<List<Integer>> cc = new ArrayList<>();
        cc.add(Arrays.asList(0));

        for (int order = 1; order != sequence.size() + 1; ++order) {
            List<List<String>> s = new ArrayList<>();
            ss.add(s);
            List<Integer> c = new ArrayList<>();
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
                            if (s.contains(skpSeq))
                                c.add(1);
                            else
                                c.add(0);
                        } else
                            c.add(col + 1);
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
                for (int col : c)
                    if (col == i)
                        ++cnt;
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
            for (int col : c)
                if (col == 0)
                    ++cnt;
            System.out.println("--- count=" + cnt);
            for (int i = 0; i != s.size(); ++i) {
                List<String> seq = s.get(i);
                int col = c.get(i);
                if (col == 0)
                    System.out.println(colorStart(col)
                            + StringUtils.join(seq, " ") + colorEnd());
            }
        }
        System.out.println("-----------");
    }
}
