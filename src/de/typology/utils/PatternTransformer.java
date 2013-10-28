package de.typology.utils;

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
}
