package de.typology.utils;

public class SequenceFormatter {

	/**
	 * Removes word at position removeWordAtPosition. Words are separated with
	 * whitespaces. Returns the resulting string.
	 * 
	 * @param inputString
	 * @param removeWordAtPosition
	 * @return
	 */
	public static String removeWord(String inputString, int removeWordAtPosition) {
		String[] words = inputString.split("\\s");
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
