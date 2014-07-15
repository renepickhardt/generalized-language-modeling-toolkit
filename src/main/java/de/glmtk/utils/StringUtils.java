package de.glmtk.utils;

import java.util.ArrayList;
import java.util.List;

public class StringUtils {

    /**
     * Takes a string and returns a list containing all substrings which are
     * separated by space.
     * 
     * This method should be used instead of string.split(' ') since it is much
     * more performant.
     */
    public static List<String> splitAtSpace(String s) {
        List<String> result = new ArrayList<String>();

        int sp1 = 0, sp2;
        while (true) {
            sp2 = s.indexOf(' ', sp1);

            if (sp2 == -1) {
                String substr = s.substring(sp1);
                if (!substr.isEmpty()) {
                    result.add(substr);
                }
                break;
            } else {
                String substr = s.substring(sp1, sp2);
                if (!substr.isEmpty()) {
                    result.add(substr);
                }
                sp1 = sp2 + 1;
            }
        }

        return result;
    }

    /**
     * Takes a list of strings and concatenates them to one, putting
     * {@code conjunction} in between.
     */
    public static String join(List<String> strings, String conjunction) {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (String string : strings) {
            if (first) {
                first = false;
            } else {
                result.append(conjunction);
            }
            result.append(string);
        }
        return result.toString();
    }

    /**
     * Takes an array of strings and concatenates them to one, putting
     * {@code conjunction} in between.
     */
    public static String join(String[] strings, String conjunction) {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (String string : strings) {
            if (first) {
                first = false;
            } else {
                result.append(conjunction);
            }
            result.append(string);
        }
        return result.toString();
    }

    /**
     * Takes a string a returns the same string repeated for given times.
     */
    public static String repeat(String string, int times) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i != times; ++i) {
            result.append(string);
        }
        return result.toString();
    }

    /**
     * Puts {@code <s1/>}, {@code <s2/>}, ... up to {@code maxPatternLength}
     * tokens around {@code line}.
     */
    public static String surroundWithTokens(int maxPatternLength, String line) {
        StringBuilder lineBuilder = new StringBuilder();
        for (int i = 1; i != maxPatternLength; ++i) {
            lineBuilder.append("<s");
            lineBuilder.append(i);
            lineBuilder.append(">/<BOS> ");
        }
        lineBuilder.append(line);
        for (int i = maxPatternLength - 1; i != 0; --i) {
            lineBuilder.append(" </s");
            lineBuilder.append(i);
            lineBuilder.append(">/<EOS>");
        }
        return lineBuilder.toString();
    }

    // TODO: revert to receive String[] split.
    public static void generateWordsAndPos(
            Object[] split,
            String[] words,
            String[] poses,
            boolean withPos) {
        for (int i = 0; i != split.length; ++i) {
            String currentWord = (String) split[i];
            if (withPos) {
                int lastSlash = currentWord.lastIndexOf('/');
                if (lastSlash == -1) {
                    words[i] = currentWord;
                    poses[i] = "UNKP"; // unkown POS, not part of any pos-tagset
                } else {
                    words[i] = currentWord.substring(0, lastSlash);
                    poses[i] = currentWord.substring(lastSlash + 1);
                }
            } else {
                words[i] = currentWord;
            }
        }
    }

}
