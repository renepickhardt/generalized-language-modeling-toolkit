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

package de.glmtk.scripts.experiments;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import de.glmtk.util.StringUtils;

/**
 * 2014-12-22 Printing several representations of the recursive GLM formula.
 */
public class E03_GlmPrintMults {
    private static List<List<String>> getSkippedHistories(List<String> history) {
        List<List<String>> skippedHistories = new LinkedList<>();
        for (int i = 0; i != history.size(); ++i) {
            if (history.get(i).equals("*"))
                continue;
            List<String> skippedHistory = new ArrayList<>(history);
            skippedHistory.set(i, "*");
            skippedHistories.add(skippedHistory);
        }
        return skippedHistories;
    }

    private static int curDenom = 1;

    private static List<List<String>> curGammas = new LinkedList<>();

    private static class AlphaWithMults {

        public List<String> alpha;

        public int denom;

        public List<List<String>> gammas;

    }

    private static List<AlphaWithMults> alphasWithMults = new LinkedList<>();

    private static void glm(String sequence,
                            List<String> history,
                            int indent) {
        AlphaWithMults alphaWithMults = new AlphaWithMults();
        alphaWithMults.alpha = new ArrayList<>(history);
        alphaWithMults.alpha.add(sequence);
        alphaWithMults.denom = curDenom;
        alphaWithMults.gammas = new ArrayList<>(curGammas);
        alphasWithMults.add(alphaWithMults);

        List<List<String>> skippedHistories = getSkippedHistories(history);
        if (skippedHistories.isEmpty()) {
            System.out.print("α(" + StringUtils.join(history, " ") + " "
                    + sequence + ")");
            return;
        }

        String indentStr = StringUtils.repeat(" ", indent);
        System.out.println("α(" + StringUtils.join(history, " ") + " "
                + sequence + ") + ");
        String out = indentStr + "1/" + skippedHistories.size() + " * γ("
                + StringUtils.join(history, " ") + ") * (";
        System.out.print(out);
        int in = out.length();
        String inStr = StringUtils.repeat(" ", in);

        curDenom *= skippedHistories.size();
        curGammas.add(history);

        boolean first = true;
        for (List<String> skippedHistory : skippedHistories)
            if (first) {
                glm(sequence, skippedHistory, in);
                first = false;
            } else {
                System.out.print(" + \n" + inStr);
                glm(sequence, skippedHistory, in);
            }
        System.out.print(")");

        curDenom /= skippedHistories.size();
        curGammas.remove(curGammas.size() - 1);
    }

    private static void printAlphaWithMults(AlphaWithMults alphaWithMults,
                                            boolean withAlpha) {
        System.out.print("1/" + alphaWithMults.denom);
        if (alphaWithMults.denom < 10)
            System.out.print(" ");
        for (List<String> gamma : alphaWithMults.gammas)
            System.out.print(" * γ(" + StringUtils.join(gamma, " ") + ")");
        if (withAlpha)
            System.out.println(" * α("
                    + StringUtils.join(alphaWithMults.alpha, " ") + ")");
    }

    public static void main(String[] args) {
        glm("e", Arrays.asList("a", "b", "c", "d"), 0);
        //        glm("Brucke", Arrays.asList("ich", "gehe", "uber", "die"), 0);

        System.out.println("\n\n" + StringUtils.repeat("-", 80) + "\n");

        int maxGammas = 0;
        int maxDenom = 0;
        for (AlphaWithMults alphaWithMults : alphasWithMults) {
            printAlphaWithMults(alphaWithMults, true);
            if (maxGammas < alphaWithMults.gammas.size())
                maxGammas = alphaWithMults.gammas.size();
            if (maxDenom < alphaWithMults.denom)
                maxDenom = alphaWithMults.denom;
        }

        System.out.println("\n" + StringUtils.repeat("-", 80) + "\n");

        Set<List<String>> alphas = new LinkedHashSet<>();
        int maxAlphaLength = 0;
        for (int i = 0; i != maxGammas + 1; ++i)
            for (AlphaWithMults alphaWithMults : alphasWithMults)
                if (alphaWithMults.gammas.size() == i) {
                    alphas.add(alphaWithMults.alpha);
                    int alphaLength = StringUtils.join(alphaWithMults.alpha,
                            " ").length();
                    if (maxAlphaLength < alphaLength)
                        maxAlphaLength = alphaLength;
                    printAlphaWithMults(alphaWithMults, true);
                }

        System.out.println("\n" + StringUtils.repeat("-", 80) + "\n");

        for (int i = 1; i != maxDenom + 1; ++i)
            for (AlphaWithMults alphaWithMults : alphasWithMults)
                if (alphaWithMults.denom == i)
                    printAlphaWithMults(alphaWithMults, true);

        System.out.println("\n" + StringUtils.repeat("-", 80) + "\n");

        int in = maxAlphaLength + 7;
        String inStr = StringUtils.repeat(" ", in);
        for (List<String> alpha : alphas) {
            String out = "α(" + StringUtils.join(alpha, " ") + ") *";
            System.out.print(out);

            boolean first = true;
            for (AlphaWithMults alphaWithMults : alphasWithMults)
                if (alpha.equals(alphaWithMults.alpha)) {
                    if (first) {
                        int diff = in - out.length() - 1;
                        System.out.print(StringUtils.repeat(" ", diff) + "(");
                        first = false;
                    } else
                        System.out.print(" + \n" + inStr);
                    printAlphaWithMults(alphaWithMults, false);
                }
            System.out.println(")");
        }
    }
}
