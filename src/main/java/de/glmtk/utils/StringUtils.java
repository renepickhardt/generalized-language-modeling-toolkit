package de.glmtk.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class StringUtils {

    /**
     * Takes a string and returns a list containing all substrings which are
     * separated by space.
     *
     * This method should be used instead of string.split(' ') since it is much
     * more performant.
     */
    public static List<String> splitAtChar(String s, char c) {
        List<String> result = new ArrayList<String>();

        int sp1 = 0, sp2;
        while (true) {
            sp2 = s.indexOf(c, sp1);

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
     * Takes a collection of objects and concatenates their string
     * representation ({@code Object#toString()}) to one, putting
     * {@code conjunction} in between.
     */
    public static String join(Collection<?> objects, String conjunction) {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (Object object : objects) {
            if (first) {
                first = false;
            } else {
                result.append(conjunction);
            }
            result.append(object.toString());
        }
        return result.toString();
    }

    /**
     * Takes an array of objects and concatenates their string representation
     * ({@code Object#toString()}) to one, putting {@code conjunction} in
     * between.
     */
    public static <T >String join(T[] objects, String conjunction) {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (T object : objects) {
            if (first) {
                first = false;
            } else {
                result.append(conjunction);
            }
            result.append(object.toString());
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

}
