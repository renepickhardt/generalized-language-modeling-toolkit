package de.typology.patterns;


/**
 * A class for transforming the used boolean pattern into different formats like
 * a binary string representation.
 * 
 * @author Martin Koerner
 * 
 */
public class PatternTransformer {

    public static String getStringPattern(boolean[] booleanPattern) {
        String stringPattern = new String();
        for (boolean bool : booleanPattern) {
            if (bool) {
                stringPattern += 1;
            } else {
                stringPattern += 0;
            }
        }
        return stringPattern;
    }

    public static boolean[] getBooleanPattern(int intPattern) {
        return PatternTransformer.getBooleanPattern(Integer
                .toBinaryString(intPattern));
    }

    public static boolean[] getBooleanPattern(String stringPattern) {
        boolean[] booleanPattern = new boolean[stringPattern.length()];
        for (int i = 0; i < stringPattern.length(); i++) {
            if (stringPattern.charAt(i) == '1') {
                booleanPattern[i] = true;
            } else {
                booleanPattern[i] = false;
            }
        }
        return booleanPattern;
    }

    public static boolean[] getBooleanPatternWithOnes(int length) {
        boolean[] booleanPattern = new boolean[length];
        for (int i = 0; i < booleanPattern.length; i++) {
            booleanPattern[i] = true;
        }
        return booleanPattern;
    }

    public static int getIntPattern(boolean[] booleanPattern) {
        String stringPattern =
                PatternTransformer.getStringPattern(booleanPattern);
        if (stringPattern.length() == 0) {
            return 0;
        } else {
            return Integer.parseInt(stringPattern, 2);
        }
    }

}
