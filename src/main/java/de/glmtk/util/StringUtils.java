package de.glmtk.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.glmtk.Constants;
import de.glmtk.counting.Tagger;

/**
 * Util class containing various static helper methods related to strings.
 */
public class StringUtils {
    /**
     * Takes a {@code string} and returns a list containing all substrings which
     * are separated by character {@code split}.
     *
     * This method should be used for space splitting instead of
     * {@code string.split(' ')} since it is much faster.
     */
    public static List<String> splitAtChar(String string,
            char split) {
        List<String> result = new ArrayList<>();

        int sp1 = 0, sp2;
        while (true) {
            sp2 = string.indexOf(split, sp1);

            if (sp2 == -1) {
                String substr = string.substring(sp1);
                if (!substr.isEmpty())
                    result.add(substr);
                break;
            }

            String substr = string.substring(sp1, sp2);
            if (!substr.isEmpty())
                result.add(substr);
            sp1 = sp2 + 1;
        }

        return result;
    }

    /**
     * Takes a collection of {@code objects} and concatenates their string
     * representation ({@code Object#toString()}) to one, putting
     * {@code conjunction} in between.
     */
    public static String join(Collection<?> objects,
                              String conjunction) {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (Object object : objects) {
            if (first)
                first = false;
            else
                result.append(conjunction);
            result.append(object.toString());
        }
        return result.toString();
    }

    /**
     * Takes an array of {@code objects} and concatenates their string
     * representation ({@code Object#toString()}) to one, putting
     * {@code conjunction} in between.
     */
    public static <T> String join(T[] objects,
                                  String conjunction) {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (T object : objects) {
            if (first)
                first = false;
            else
                result.append(conjunction);
            result.append(object.toString());
        }
        return result.toString();
    }

    /**
     * Takes a {@code string} and concatenates that string repeatedly for given
     * {@code times}.
     */
    public static String repeat(String string,
                                int times) {
        if (times < 0)
            throw new IllegalArgumentException(
                    String.format(
                            "Argument 'times' needs to be greater or equal to zero, was '%d'.",
                            times));
        StringBuilder result = new StringBuilder();
        for (int i = 0; i != times; ++i)
            result.append(string);
        return result.toString();
    }

    public static String replaceAll(String string,
                                    String target,
                                    String replace) {
        if (target.length() != replace.length())
            throw new IllegalArgumentException(
                    "<target> and <replace> need to be of same length.");

        String result = string;
        for (int i = 0; i != target.length(); ++i)
            result = result.replace(target.charAt(i), replace.charAt(i));
        return result;
    }

    public static String capitalize(String string) {
        return Character.toUpperCase(string.charAt(0)) + string.substring(1);
    }

    /**
     * Puts {@code <s1/>}, {@code <s2/>}, ... up to {@code maxPatternLength}
     * tokens around {@code line}.
     */
    @Deprecated
    public static String surroundWithTokens(int maxPatternLength,
                                            String line) {
        StringBuilder lineBuilder = new StringBuilder();
        for (int i = 1; i != maxPatternLength; ++i)
            lineBuilder.append("<s").append(i).append(">/<BOS> ");
        lineBuilder.append(line);
        for (int i = maxPatternLength - 1; i != 0; --i)
            lineBuilder.append(" </s").append(i).append(">/<EOS>");
        return lineBuilder.toString();
    }

    /**
     * Extracts words and part-of-speeches from a given sequence of words.
     *
     * @param split
     *            An array of either words or word/part-of-speech combinations.
     * @param hasPos
     *            Whether {@code split} contains part-of-speeches
     * @param words
     *            Expects to be given an array of same size as {@code split},
     *            will put add words found into this.
     * @param poses
     *            Expects to be given an array of same size as {@code split},
     *            will put add part-of-speeches found into this. If a
     *            part-of-speech is missing, it will contain
     *            {@link Constants#UNKOWN_POS} instead.
     */
    public static void extractWordsAndPoses(String[] split,
                                            boolean hasPos,
                                            String[] words,
                                            String[] poses) {
        for (int i = 0; i != split.length; ++i) {
            String word = split[i];
            if (hasPos) {
                int lastSlash = word.lastIndexOf(Tagger.POS_SEPARATOR);
                if (lastSlash == -1) {
                    words[i] = word;
                    poses[i] = Constants.UNKOWN_POS;
                } else {
                    words[i] = word.substring(0, lastSlash);
                    poses[i] = word.substring(lastSlash + 1);
                }
            } else {
                words[i] = word;
                poses[i] = Constants.UNKOWN_POS;
            }
        }
    }
}
