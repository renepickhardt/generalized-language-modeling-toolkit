package de.typology.patterns;

import java.util.ArrayList;

public class PatternBuilder {

    public static ArrayList<boolean[]> getGLMPatterns(int maxModelLength) {
        ArrayList<boolean[]> patterns = new ArrayList<boolean[]>();
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

    public static ArrayList<boolean[]>
        getReverseGLMPatterns(int maxModelLength) {
        ArrayList<boolean[]> patterns = new ArrayList<boolean[]>();
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
    public static ArrayList<boolean[]> getGLMForSmoothingPatterns(
            int maxModelLength) {
        ArrayList<boolean[]> patterns = new ArrayList<boolean[]>();
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

    public static ArrayList<boolean[]> getReverseGLMForSmoothingPatterns(
            int maxModelLength) {
        ArrayList<boolean[]> patterns = new ArrayList<boolean[]>();
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

    public static ArrayList<boolean[]> getLMPatterns(int maxModelLength) {
        ArrayList<boolean[]> patterns = new ArrayList<boolean[]>();
        for (int intPattern = 1; intPattern < Math.pow(2, maxModelLength); intPattern++) {
            String stringPattern = Integer.toBinaryString(intPattern);
            if (Integer.bitCount(intPattern) == stringPattern.length()) {
                patterns.add(PatternTransformer.getBooleanPattern(intPattern));
            }
        }
        return patterns;
    }

    public static ArrayList<boolean[]> getReverseLMPatterns(int maxModelLength) {
        ArrayList<boolean[]> patterns = new ArrayList<boolean[]>();
        for (int intPattern = (int) (Math.pow(2, maxModelLength) - 1); intPattern > 0; intPattern--) {
            String stringPattern = Integer.toBinaryString(intPattern);
            if (Integer.bitCount(intPattern) == stringPattern.length()) {
                patterns.add(PatternTransformer.getBooleanPattern(intPattern));
            }
        }
        return patterns;
    }

    public static ArrayList<boolean[]> getTypologyPatterns(int maxModelLength) {
        ArrayList<boolean[]> patterns = new ArrayList<boolean[]>();
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

    public static ArrayList<boolean[]> getReverseTypologyPatterns(
            int maxModelLength) {
        ArrayList<boolean[]> patterns = new ArrayList<boolean[]>();
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
