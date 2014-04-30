package de.typology.patterns;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PatternBuilder {

    /**
     * Creates all permutations of { true, false } from length 1 to
     * maxModeLength. Permutations with leading or trailing ranges of falses
     * are ignored.
     */
    public static List<boolean[]> getGLMPatterns(int maxModelLength) {
        int pow = (int) Math.pow(2, maxModelLength);
        List<boolean[]> patterns = new ArrayList<boolean[]>(pow);
        for (int intPattern = 1; intPattern < pow; ++intPattern) {
            // leave out even sequences since they don't contain a
            // target
            if (intPattern % 2 == 0) {
                continue;
            }
            patterns.add(PatternTransformer.getBooleanPattern(intPattern));
        }
        return patterns;
    }

    /**
     * As {@link #getGLMPatterns(int)} but reversed.
     */
    public static List<boolean[]> getReverseGLMPatterns(int maxModelLength) {
        List<boolean[]> patterns = getGLMPatterns(maxModelLength);
        Collections.reverse(patterns);
        return patterns;
    }

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
     *   { true }            // 1 - time
     *   { true, true}       // 2 - times
     *   ...
     *   { true, true, ... } // n -times
     * }
     * </pre>
     */
    public static List<boolean[]> getLMPatterns(int maxModelLength) {
        List<boolean[]> patterns = new ArrayList<boolean[]>(maxModelLength);
        for (int i = 0; i != maxModelLength; ++i) {
            boolean[] pattern = new boolean[i + 1];
            for (int j = 0; j != i + 1; ++j) {
                pattern[j] = true;
            }
            patterns.add(pattern);
        }
        return patterns;
    }

    /**
     * As {@link #getLMPatterns(int)} but reversed.
     */
    public static List<boolean[]> getReverseLMPatterns(int maxModelLength) {
        List<boolean[]> patterns = getLMPatterns(maxModelLength);
        Collections.reverse(patterns);
        return patterns;
    }

    /**
     * Create pattern like these (n = maxModelLength):
     * 
     * <pre>
     * {
     *   { true }                           // length: 1
     *   { true, true }                     // length: 2
     *   { true, false, true }              // length: 3
     *   { true, false, false, true }       // length: 4
     *   ...
     *   { true, false, ... , false, true } // length: n
     * }
     * </pre>
     */
    public static List<boolean[]> getTypologyPatterns(int maxModelLength) {
        List<boolean[]> patterns = new ArrayList<boolean[]>(maxModelLength);
        for (int i = 0; i != maxModelLength; ++i) {
            boolean[] pattern = new boolean[i + 1];
            pattern[0] = true;
            if (i != 0) {
                for (int j = 1; j != i; ++j) {
                    pattern[j] = false;
                }
            }
            pattern[i] = true;
            patterns.add(pattern);
        }
        return patterns;
    }

    /**
     * As {@link #getTypologyPatterns(int)} but reversed.
     */
    public static List<boolean[]>
        getReverseTypologyPatterns(int maxModelLength) {
        List<boolean[]> patterns = getTypologyPatterns(maxModelLength);
        Collections.reverse(patterns);
        return patterns;
    }

}
