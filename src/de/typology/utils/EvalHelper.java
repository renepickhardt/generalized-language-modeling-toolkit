package de.typology.utils;

public class EvalHelper {

	public static boolean badLine(String[] words, int n) {
		if (words.length < n) {
			return true;
		}
		for (int l = 0; l < n; l++) {
			if (words[l].length() < 1) {
				return true;
			}
			// if (words[l].contains("?")) {
			// System.err.println("Query word contains ?: " + words[l]);
			// return true;
			// }
		}
		return false;
	}
}
