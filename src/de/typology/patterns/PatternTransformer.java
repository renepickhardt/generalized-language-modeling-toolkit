package de.typology.patterns;

import java.util.Arrays;

/**
 * A class for transforming the used boolean pattern into different formats like
 * a binary string representation.
 * 
 * @author Martin Koerner
 * 
 */
public class PatternTransformer {

	public static void main(String[] args) {
		boolean[] bool1 = { true, false, true, true };
		System.out.println(getStringPattern(bool1));
		int i = 8;
		boolean[] bool2 = PatternTransformer.getBooleanPattern(Integer
				.toBinaryString(i));
		System.out.println(Arrays.toString(bool2));
	}

	public static String getStringPattern(boolean[] pattern) {
		String stringPattern = new String();
		for (boolean bool : pattern) {
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
}
