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

import de.glmtk.util.ArrayUtils;
import de.glmtk.util.StringUtils;

/**
 * 2014-12-27
 */
public class E06_GlmGenMultsDist {

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

    private static boolean equals(boolean[] lhs,
                                  boolean[] rhs) {
        for (int i = 0; i != lhs.length; ++i)
            if (lhs[i] != rhs[i])
                return false;
        return true;
    }

    private static List<Object> generateMults(boolean[] pattern,
                                              boolean[] hpattern,
                                              List<String> fullHistory) {
        if (equals(pattern, hpattern))
            return null;

        List<Object> mults = new ArrayList<>();
        mults.add(getPatternedHistory(fullHistory, pattern));
        List<List<Object>> subMults = new ArrayList<>();
        for (int i = 0; i != pattern.length; ++i)
            if (hpattern[i] && !pattern[i]) {
                boolean[] newPattern = Arrays.copyOf(pattern, pattern.length);
                newPattern[i] = true;
                subMults.add(generateMults(newPattern, hpattern, fullHistory));
            }
        mults.add(subMults);
        return mults;
    }

    private static void printMults(List<Object> mults,
                                   int indent) {
        if (mults == null) {
            System.out.print("(1)");
            return;
        }

        @SuppressWarnings("unchecked")
        List<String> gamma = (List<String>) mults.get(0);
        @SuppressWarnings("unchecked")
        List<List<Object>> subMults = (List<List<Object>>) mults.get(1);

        String out = "(γ(" + StringUtils.join(gamma, " ") + ")";

        if (subMults.isEmpty())
            System.out.print(out);
        else {

            boolean first = true;
            for (List<Object> subMult : subMults) {
                if (first) {
                    first = false;
                    if (subMult != null)
                        out += " * ";
                    System.out.print(out);
                } else
                    System.out.print(" + \n"
                            + StringUtils.repeat(" ", indent + out.length()));
                if (subMult != null)
                    printMults(subMult, indent + out.length());
            }
        }

        System.out.print(")");
    }

    private static void generateAlphasWithMults(String sequence,
                                                List<String> history) {
        List<List<String>> histories = generateHistories(history);
        for (List<String> h : histories) {
            List<Object> gammas = generateMults(getPattern(history),
                    getPattern(h), history);
            String out = "α(" + StringUtils.join(h, " ") + " " + sequence
                    + ") * ";
            System.out.print(out);
            printMults(gammas, out.length());
            System.out.println();
        }
    }

    public static void main(String[] args) {
        //        generateAlphasWithMults("Brucke", Arrays.asList("gehe", "uber", "die"));
        generateAlphasWithMults("d", Arrays.asList("a", "b", "c"/* , "d" */));
    }

}
