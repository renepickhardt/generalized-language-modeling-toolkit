package de.typology.patterns;

import java.util.ArrayList;
import java.util.List;

public class PatternBuilder {

    public static List<boolean[]> getGLMPatterns(int maxModelLength) {
        List<boolean[]> patterns = new ArrayList<boolean[]>();
        for (int intPattern = 1; intPattern < Math.pow(2, maxModelLength); intPattern++) {
            // leave out even sequences since they don't contain a
            // target
            if (intPattern % 2 == 0) {
                continue;
            }
            patterns.add(PatternTransformer.getBooleanPattern(intPattern));
        }
        return patterns;
    }

    public static List<boolean[]> getReverseGLMPatterns(int maxModelLength) {
        List<boolean[]> patterns = new ArrayList<boolean[]>();
        for (int intPattern = (int) (Math.pow(2, maxModelLength) - 1); intPattern > 0; intPattern--) {
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
     * Also returns sequences that are greater than maxModelLength but are
     * needed to calculate kneser ney smoothed values
     * 
     * @param maxModelLength
     * @return
     */
    public static List<boolean[]>
        getGLMForSmoothingPatterns(int maxModelLength) {
        List<boolean[]> patterns = new ArrayList<boolean[]>();
        for (int intPattern = 1; intPattern < Math.pow(2, maxModelLength); intPattern++) {
            // // leave out even sequences since they don't contain a
            // // target
            // if (intPattern % 2 == 0) {
            // continue;
            // }
            patterns.add(PatternTransformer.getBooleanPattern(intPattern));
        }
        return patterns;
    }

    public static List<boolean[]> getReverseGLMForSmoothingPatterns(
            int maxModelLength) {
        List<boolean[]> patterns = new ArrayList<boolean[]>();
        for (int intPattern = (int) Math.pow(2, maxModelLength) - 1; intPattern > 0; intPattern--) {
            // // leave out even sequences since they don't contain a
            // // target
            // if (intPattern % 2 == 0) {
            // continue;
            // }
            patterns.add(PatternTransformer.getBooleanPattern(intPattern));
        }
        return patterns;
    }

    public static List<boolean[]> getLMPatterns(int maxModelLength) {
        List<boolean[]> patterns = new ArrayList<boolean[]>();
        for (int intPattern = 1; intPattern < Math.pow(2, maxModelLength); intPattern++) {
            String stringPattern = Integer.toBinaryString(intPattern);
            if (Integer.bitCount(intPattern) == stringPattern.length()) {
                patterns.add(PatternTransformer.getBooleanPattern(intPattern));
            }
        }
        return patterns;
    }

    /**
     * Creates pattern like these(n = maxModelLength):
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
        List<boolean[]> patterns = new ArrayList<boolean[]>();
        for (int intPattern = (int) (Math.pow(2, maxModelLength) - 1); intPattern > 0; intPattern--) {
            String stringPattern = Integer.toBinaryString(intPattern);
            if (Integer.bitCount(intPattern) == stringPattern.length()) {
                patterns.add(PatternTransformer.getBooleanPattern(intPattern));
            }
        }
        return patterns;
    }

    public static List<boolean[]> getTypologyPatterns(int maxModelLength) {
        List<boolean[]> patterns = new ArrayList<boolean[]>();
        for (int intPattern = 1; intPattern < Math.pow(2, maxModelLength); intPattern++) {
            String stringPattern = Integer.toBinaryString(intPattern);
            if (Integer.bitCount(intPattern) <= 2
                    && stringPattern.startsWith("1")
                    && stringPattern.endsWith("1")) {
                patterns.add(PatternTransformer.getBooleanPattern(intPattern));
            }
        }
        return patterns;
    }

    public static List<boolean[]>
        getReverseTypologyPatterns(int maxModelLength) {
        List<boolean[]> patterns = new ArrayList<boolean[]>();
        for (int intPattern = (int) (Math.pow(2, maxModelLength) - 1); intPattern > 0; intPattern--) {
            String stringPattern = Integer.toBinaryString(intPattern);
            if (Integer.bitCount(intPattern) <= 2
                    && stringPattern.startsWith("1")
                    && stringPattern.endsWith("1")) {
                patterns.add(PatternTransformer.getBooleanPattern(intPattern));
            }
        }
        return patterns;
    }
}
