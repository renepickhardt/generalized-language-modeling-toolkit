package de.typology.patterns;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PatternBuilder {

    /**
     * Also returns sequences that are greater than maxModelLength but are
     * needed to calculate kneser ney smoothed values
     */
    public static List<boolean[]>
        getGLMForSmoothingPatterns(int maxModelLength) {
        int pow = (int) Math.pow(2, maxModelLength);
        List<boolean[]> patterns = new ArrayList<boolean[]>(pow);
        for (int intPattern = 1; intPattern < pow; intPattern++) {
            patterns.add(PatternTransformer.getBooleanPattern(intPattern));
        }
        return patterns;
    }

    /**
     * As {@link #getGLMForSmoothingPatterns(int)} but reversed.
     */
    public static List<boolean[]> getReverseGLMForSmoothingPatterns(
            int maxModelLength) {
        List<boolean[]> patterns = getGLMForSmoothingPatterns(maxModelLength);
        Collections.reverse(patterns);
        return patterns;
    }

    /**
     * Creates pattern like these (n = maxModelLength):
     * 
     * <pre>
     * {
     *   { true, true, ... } // n     - times
     *   { true, ... }       // (n-1) - times
     *   ...
     *   { true }            // 1     - time
     * }
     * </pre>
     */
    public static List<boolean[]> getReverseLMPatterns(int maxModelLength) {
        List<boolean[]> patterns = new ArrayList<boolean[]>(maxModelLength);
        for (int i = maxModelLength; i != 0; --i) {
            boolean[] pattern = new boolean[i + 1];
            for (int j = 0; j != i + 1; ++j) {
                pattern[j] = true;
            }
            patterns.add(pattern);
        }
        return patterns;
    }

}
