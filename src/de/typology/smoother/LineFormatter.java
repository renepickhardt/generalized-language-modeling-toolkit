package de.typology.smoother;

public class LineFormatter {

	/*
	 * Removes the count that is separated with the delimiter and the word at
	 * position removeWordAtPosition. Words are separated with whitespaces.
	 * Returns the resulting string.
	 */
	public static String removeWord(String inputString, String countDelimiter,
			int removeWordAtPosition) {
		String[] words = inputString.split(countDelimiter)[0].split("\\s");
		String result = "";
		for (int i = 0; i < words.length; i++) {
			if (i != removeWordAtPosition) {
				result += words[i] + " ";
			}
		}
		result = result.replaceFirst(" $", "");
		return result;
	}

}
