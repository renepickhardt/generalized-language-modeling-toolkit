package de.glmtk.experiments;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import de.glmtk.utils.StringUtils;

/**
 * 2014-12-22
 * Printing several representations of the recursive GLM formula.
 */
public class E03_GlmPrintMults {

    private static List<List<String>> getSkippedHistories(List<String> history) {
        List<List<String>> skippedHistories = new LinkedList<List<String>>();
        for (int i = 0; i != history.size(); ++i) {
            if (history.get(i).equals("*")) {
                continue;
            }
            List<String> skippedHistory = new ArrayList<String>(history);
            skippedHistory.set(i, "*");
            skippedHistories.add(skippedHistory);
        }
        return skippedHistories;
    }

    private static int curDenom = 1;

    private static List<List<String>> curGammas =
            new LinkedList<List<String>>();

    private static class AlphaWithMults {

        public List<String> alpha;

        public int denom;

        public List<List<String>> gammas;

    }

    private static List<AlphaWithMults> alphasWithMults =
            new LinkedList<AlphaWithMults>();

    private static void glm(String sequence, List<String> history, int indent) {
        AlphaWithMults alphaWithMults = new AlphaWithMults();
        alphaWithMults.alpha = new ArrayList<String>(history);
        alphaWithMults.alpha.add(sequence);
        alphaWithMults.denom = curDenom;
        alphaWithMults.gammas = new ArrayList<List<String>>(curGammas);
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
        String out =
                indentStr + "1/" + skippedHistories.size() + " * γ("
                        + StringUtils.join(history, " ") + ") * (";
        System.out.print(out);
        int in = out.length();
        String inStr = StringUtils.repeat(" ", in);

        curDenom *= skippedHistories.size();
        curGammas.add(history);

        boolean first = true;
        for (List<String> skippedHistory : skippedHistories) {
            if (first) {
                glm(sequence, skippedHistory, in);
                first = false;
            } else {
                System.out.print(" + \n" + inStr);
                glm(sequence, skippedHistory, in);
            }
        }
        System.out.print(")");

        curDenom /= skippedHistories.size();
        curGammas.remove(curGammas.size() - 1);
    }

    private static void printAlphaWithMults(
            AlphaWithMults alphaWithMults,
            boolean withAlpha) {
        System.out.print("1/" + alphaWithMults.denom);
        if (alphaWithMults.denom < 10) {
            System.out.print(" ");
        }
        for (List<String> gamma : alphaWithMults.gammas) {
            System.out.print(" * γ(" + StringUtils.join(gamma, " ") + ")");
        }
        if (withAlpha) {
            System.out.println(" * α("
                    + StringUtils.join(alphaWithMults.alpha, " ") + ")");
        }
    }

    public static void main(String[] args) {
        glm("e", Arrays.asList("a", "b", "c", "d"), 0);
        //        glm("Brucke", Arrays.asList("ich", "gehe", "uber", "die"), 0);

        System.out.println("\n\n" + StringUtils.repeat("-", 80) + "\n");

        int maxGammas = 0;
        int maxDenom = 0;
        for (AlphaWithMults alphaWithMults : alphasWithMults) {
            printAlphaWithMults(alphaWithMults, true);
            if (maxGammas < alphaWithMults.gammas.size()) {
                maxGammas = alphaWithMults.gammas.size();
            }
            if (maxDenom < alphaWithMults.denom) {
                maxDenom = alphaWithMults.denom;
            }
        }

        System.out.println("\n" + StringUtils.repeat("-", 80) + "\n");

        Set<List<String>> alphas = new LinkedHashSet<List<String>>();
        int maxAlphaLength = 0;
        for (int i = 0; i != maxGammas + 1; ++i) {
            for (AlphaWithMults alphaWithMults : alphasWithMults) {
                if (alphaWithMults.gammas.size() == i) {
                    alphas.add(alphaWithMults.alpha);
                    int alphaLength =
                            StringUtils.join(alphaWithMults.alpha, " ")
                                    .length();
                    if (maxAlphaLength < alphaLength) {
                        maxAlphaLength = alphaLength;
                    }
                    printAlphaWithMults(alphaWithMults, true);
                }
            }
        }

        System.out.println("\n" + StringUtils.repeat("-", 80) + "\n");

        for (int i = 1; i != maxDenom + 1; ++i) {
            for (AlphaWithMults alphaWithMults : alphasWithMults) {
                if (alphaWithMults.denom == i) {
                    printAlphaWithMults(alphaWithMults, true);
                }
            }
        }

        System.out.println("\n" + StringUtils.repeat("-", 80) + "\n");

        int in = maxAlphaLength + 7;
        String inStr = StringUtils.repeat(" ", in);
        for (List<String> alpha : alphas) {
            String out = "α(" + StringUtils.join(alpha, " ") + ") *";
            System.out.print(out);

            boolean first = true;
            for (AlphaWithMults alphaWithMults : alphasWithMults) {
                if (alpha.equals(alphaWithMults.alpha)) {
                    if (first) {
                        int diff = in - out.length() - 1;
                        System.out.print(StringUtils.repeat(" ", diff) + "(");
                        first = false;
                    } else {
                        System.out.print(" + \n" + inStr);
                    }
                    printAlphaWithMults(alphaWithMults, false);
                }
            }
            System.out.println(")");
        }
    }
}

//α(a b c d e) * 1/1  * (1)
//α(* b c d e) * 1/4  * (γ(a b c d))
//α(a * c d e) * 1/4  * (γ(a b c d))
//α(a b * d e) * 1/4  * (γ(a b c d))
//α(a b c * e) * 1/4  * (γ(a b c d))
//α(* * c d e) * 1/12 * (γ(a b c d) * (γ(* b c d) +
//                                     γ(a * c d)))
//α(* b * d e) * 1/12 * (γ(a b c d) * (γ(* b c d) +
//                                     γ(a b * d)))
//α(* b c * e) * 1/12 * (γ(a b c d) * (γ(* b c d) +
//                                     γ(a b c *)))
//α(a * * d e) * 1/12 * (γ(a b c d) * (γ(a * c d) +
//                                     γ(a b * d)))
//α(a * c * e) * 1/12 * (γ(a b c d) * (γ(a * c d) +
//                                     γ(a b c *)))
//α(a b * * e) * 1/12 * (γ(a b c d) * (γ(a b * d) +
//                                     γ(a b c *)))
//α(* * * d e) * 1/24 * (γ(a b c d) * (γ(* b c d) * (γ(* * c d) +
//                                                   γ(* b * d)) +
//                                     γ(a * c d) * (γ(* * c d) +
//                                                   γ(a * * d)) +
//                                     γ(a b * d) * (γ(* b * d) +
//                                                   γ(a * * d))))
//α(* * c * e) * 1/24 * (γ(a b c d) * (γ(* b c d) * (γ(* * c d) +
//                                                   γ(* b c *)) +
//                                     γ(a * c d) * (γ(* * c d) +
//                                                   γ(a * c *)) +
//                                     γ(a b c *) * (γ(* b c *) +
//                                                   γ(a * c *))))
//α(* b * * e) * 1/24 * (γ(a b c d) * (γ(* b c d) * (γ(* b * d) +
//                                                   γ(* b c *)) +
//                                     γ(a b * d) * (γ(* b * d) +
//                                                   γ(a b * *)) +
//                                     γ(a b c *) * (γ(* b c *) +
//                                                   γ(a b * *))))
//α(a * * * e) * 1/24 * (γ(a b c d) * (γ(a * c d) * (γ(a * * d) +
//                                                   γ(a * c *)) +
//                                     γ(a b * d) * (γ(a * * d) +
//                                                   γ(a b * *)) +
//                                     γ(a b c *) * (γ(a * c *) +
//                                                   γ(a b * *))))
//α(* * * * e) * 1/24 * (γ(a b c d) * (γ(* b c d) * (γ(* * c d) * (γ(* * * d) +
//                                                                 γ(* * c *)) +
//                                                   γ(* b * d) * (γ(* * * d) +
//                                                                 γ(* b * *)) +
//                                                   γ(* b c *) * (γ(* * c *) +
//                                                                 γ(* b * *))) +
//                                     γ(a * c d) * (γ(* * c d) * (γ(* * * d) +
//                                                                 γ(* * c *)) +
//                                                   γ(a * * d) * (γ(* * * d) +
//                                                                 γ(a * * *)) +
//                                                   γ(a * c *) * (γ(* * c *) +
//                                                                 γ(a * * *))) +
//                                     γ(a b * d) * (γ(* b * d) * (γ(* * * d) +
//                                                                 γ(* b * *)) +
//                                                   γ(a * * d) * (γ(* * * d) +
//                                                                 γ(a * * *)) +
//                                                   γ(a b * *) * (γ(* b * *) +
//                                                                 γ(a * * *))) +
//                                     γ(a b c *) * (γ(* b c *) * (γ(* * c *) +
//                                                                 γ(* b * *)) +
//                                                   γ(a * c *) * (γ(* * c *) +
//                                                                 γ(a * * *)) +
//                                                   γ(a b * *) * (γ(* b * *) +
//                                                                 γ(a * * *)))))