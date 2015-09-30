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
import java.util.List;

import de.glmtk.util.ArrayUtils;
import de.glmtk.util.StringUtils;


/**
 * Didn't work.
 */
public class E05_GlmGenComb {

    private static List<String> getPatternedHistory(List<String> history,
                                                    boolean[] pattern) {
        List<String> result = new ArrayList<>(history);
        for (int i = 0; i != pattern.length; ++i) {
            if (pattern[i]) {
                result.set(i, "*");
            }
        }
        return result;
    }

    private static boolean nextPattern(boolean[] pattern) {
        int first = 0, last = pattern.length;
        for (int i = last - 2; i != first - 1; --i) {
            if (pattern[i] && !pattern[i + 1]) { // pattern[i] > pattern[ii]
                int j = last - 1;
                while (!pattern[i] || pattern[j]) {
                    --j;
                }
                ArrayUtils.swap(pattern, i, j);
                ArrayUtils.reverse(pattern, i + 1, last);
                return true;
            }
        }
        ArrayUtils.reverse(pattern, first, last);
        for (int i = 0; i != pattern.length; ++i) {
            if (!pattern[i]) {
                pattern[i] = true;
                return true;
            }
        }
        return false;
    }

    private static long factorial(long n) {
        return n <= 1 ? 1 : n * factorial(n - 1);
    }

    private static int numOrder(int n,
                                int order) {
        return (int) (factorial(n) / (factorial(order) * factorial(n - order)));
    }

    private static List<List<String>> alphas;

    private static List<List<List<List<String>>>> gammasss;

    private static void genComb(String sequence,
                                List<String> history) {
        int numAlphas = (int) Math.pow(2, history.size());
        alphas = new ArrayList<>(numAlphas);
        gammasss = new ArrayList<>(numAlphas);
        for (int i = 0; i != numAlphas; ++i) {
            alphas.add(null);
            gammasss.add(new ArrayList<List<List<String>>>());
        }

        boolean[] pattern = new boolean[history.size()];
        for (int i = 0; i != pattern.length; ++i) {
            pattern[i] = false;
        }

        int order = 0;
        int numOrder = numOrder(history.size(), order);
        int orderEnd = numOrder;

        for (int i = 0, iInOrder = 0; i != numAlphas; ++i, ++iInOrder) {
            if (i >= orderEnd) {
                ++order;
                iInOrder = 0;
                numOrder = numOrder(history.size(), order);
                orderEnd += numOrder;
            }
            List<String> h = getPatternedHistory(history, pattern);
            List<String> alpha = ArrayUtils.unionWithSingleton(h, sequence);
            alphas.set(i, alpha);
            for (int j = orderEnd; j != numAlphas; ++j) {
                // gammasss.get(j).add(alpha);
                if (i == 0) {
                    gammasss.get(j).add(Arrays.asList(alpha));
                    continue;
                }
                if (iInOrder == 0) {
                    List<List<List<String>>> gammass = gammasss.get(j);
                    List<List<List<String>>> newGammass =
                        new ArrayList<>(gammass.size() * numOrder);
                    for (int k = 0; k != gammass.size(); ++k) {
                        List<List<String>> gammas = gammass.get(k);
                        for (int l = 0; l != numOrder; ++l) {
                            newGammass.add(new ArrayList<>(gammas));
                        }
                    }
                    gammasss.set(j, newGammass);
                }
                gammasss.get(j).get(iInOrder).add(alpha);
            }
            nextPattern(pattern);
        }
    }

    public static void main(String[] args) {
        genComb("d", Arrays.asList("a", "b", "c"));

        for (int i = 0; i != alphas.size(); ++i) {
            List<String> alpha = alphas.get(i);
            List<List<List<String>>> gammass = gammasss.get(i);

            String alphaStr =
                alpha == null ? "null" : StringUtils.join(alpha, " ");
            System.out.println("α(" + alphaStr + ")");
            if (gammass != null) {
                for (List<List<String>> gammas : gammass) {
                    if (gammas != null) {
                        for (List<String> gamma : gammas) {
                            String gammaStr = gamma == null
                                ? "null" : StringUtils.join(gamma, " ");
                            System.out.print("  γ(" + gammaStr + ")");
                        }
                        System.out.println();
                    }
                }
            }
        }
    }

}
