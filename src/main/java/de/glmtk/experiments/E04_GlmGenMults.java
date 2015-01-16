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
import java.util.LinkedList;
import java.util.List;

import de.glmtk.util.StringUtils;
import de.glmtk.utils.ArrayUtils;

/**
 * 2014-12-23 Generating all gamma multiplications for a given alpha.
 *
 * <p>
 * Result: For histories of length n there are exactly 2^n skipped histories.
 */
public class E04_GlmGenMults {

    private static boolean[] getPattern(int size,
                                        int order) {
        boolean[] pattern = new boolean[size];
        for (int i = 0; i != order; ++i)
            pattern[i] = true;
        for (int i = order; i != size; ++i)
            pattern[i] = false;
        return pattern;
    }

    private static boolean[] getPattern(List<String> history) {
        boolean[] pattern = new boolean[history.size()];
        int i = 0;
        for (String token : history) {
            pattern[i] = token.equals("*") ? true : false;
            ++i;
        }
        return pattern;
    }

    private static List<String> getPatternedHistory(List<String> history,
                                                    boolean[] pattern) {
        List<String> skippedHistory = new ArrayList<>(history);
        int i = 0;
        for (boolean p : pattern) {
            if (p)
                skippedHistory.set(i, "*");
            ++i;
        }
        return skippedHistory;
    }

    private static List<List<String>> generateHistories(List<String> history) {
        List<List<String>> histories = new LinkedList<>();

        for (int order = 0; order != history.size() + 1; ++order) {
            boolean[] pattern = getPattern(history.size(), order);
            histories.add(getPatternedHistory(history, pattern));

            int first = 0, last = history.size();
            for (int i = last - 2; i != first - 1; --i)
                if (pattern[i] && !pattern[i + 1]) { // pattern[i] > pattern[ii]
                    int j = last - 1;
                    while (!pattern[i] || pattern[j])
                        --j;
                    ArrayUtils.swap(pattern, i, j);
                    ArrayUtils.reverse(pattern, i + 1, last);
                    histories.add(getPatternedHistory(history, pattern));
                    i = last - 1;
                }
        }

        return histories;
    }

    @SuppressWarnings("unused")
    private static void generateMultPatterns(boolean[] pattern,
                                             List<boolean[]> curMultPattern,
                                             List<List<boolean[]>> multsPattern) {
        List<boolean[]> c = new LinkedList<boolean[]>();
        c.addAll(curMultPattern);
        c.add(pattern);

        boolean new_ = false;
        for (int i = pattern.length - 1; i != -1; --i)
            if (pattern[i]) {
                new_ = true;
                boolean[] newPattern = Arrays.copyOf(pattern, pattern.length);
                newPattern[i] = false;
                generateMultPatterns(newPattern, c, multsPattern);
            }

        if (!new_) {
            c.remove(0);
            multsPattern.add(c);
        }
    }

    public static List<List<boolean[]>> generateMultPatterns2(boolean[] pattern) {
        List<List<boolean[]>> result = new LinkedList<>();
        for (int i = pattern.length - 1; i != -1; --i)
            if (pattern[i]) {
                boolean[] newPattern = Arrays.copyOf(pattern, pattern.length);
                newPattern[i] = false;
                result.addAll(generateMultPattern2Hat(newPattern,
                        new ArrayList<boolean[]>()));
            }
        return result;
    }

    public static List<List<boolean[]>> generateMultPattern2Hat(boolean[] pattern,
                                                                List<boolean[]> prev) {
        List<boolean[]> newPrev = ArrayUtils.unionWithSingleton(prev, pattern);

        List<List<boolean[]>> result = new LinkedList<>();
        for (int i = pattern.length - 1; i != -1; --i)
            if (pattern[i]) {
                boolean[] newPattern = Arrays.copyOf(pattern, pattern.length);
                newPattern[i] = false;
                result.addAll(generateMultPattern2Hat(newPattern, newPrev));
            }

        if (result.isEmpty())
            result.add(newPrev);
        return result;
    }

    private static List<List<List<String>>> generateMults(List<String> history,
                                                          List<String> fullHistory) {
        //        List<List<boolean[]>> multsPattern = new LinkedList<List<boolean[]>>();
        //        generateMultPatterns(getPattern(history), new LinkedList<boolean[]>(),
        //                multsPattern);
        List<List<boolean[]>> multsPattern = generateMultPatterns2(getPattern(history));
        List<List<List<String>>> mults = new ArrayList<>(multsPattern.size());
        for (List<boolean[]> multPattern : multsPattern) {
            List<List<String>> mult = new LinkedList<>();
            mults.add(mult);
            for (boolean[] pattern : multPattern)
                mult.add(getPatternedHistory(fullHistory, pattern));
        }
        return mults;
    }

    private static void generateAlphasWithMults(String sequence,
                                                List<String> history) {
        List<List<String>> histories = generateHistories(history);
        for (List<String> h : histories) {
            List<List<List<String>>> gammas = generateMults(h, history);
            System.out.println("α(" + StringUtils.join(h, " ") + " " + sequence
                    + ")");
            for (List<List<String>> g : gammas) {
                for (List<String> gamma : g)
                    System.out.print("    γ(" + StringUtils.join(gamma, " ")
                            + ") ");
                System.out.println();
            }
        }
    }

    public static void main(String[] args) {
        //        generateAlphasWithMults("Brucke", Arrays.asList("gehe", "uber", "die"));
        generateAlphasWithMults("e", Arrays.asList("a", "b", "c", "d"));
    }

}
