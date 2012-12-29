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
		}
		return false;
	}

	public static String prepareQuery(String[] words, int n) {
		String query = "";
		int l = words.length;
		for (int i = l - n; i < l - 1; i++) {
			query = query + words[i] + " ";
		}
		query = query.substring(0, query.length() - 1);
		return query;
	}
}
