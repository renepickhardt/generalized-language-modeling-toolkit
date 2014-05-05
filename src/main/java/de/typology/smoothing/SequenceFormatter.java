package de.typology.smoothing;

import de.typology.patterns.Pattern;
import de.typology.patterns.PatternElem;

public class SequenceFormatter {

    /**
     * Removes word at position removeWordAtPosition. Words are separated with
     * whitespaces. Returns the resulting string.
     * 
     * @param inputString
     * @param removeWordAtPosition
     * @return
     */
    public static String
        removeWord(String inputString, int removeWordAtPosition) {
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

    public static String removeWords(String inputString, Pattern pattern) {
        String[] words = inputString.split("\\s");

        if (words.length == pattern.length()) {
            String resultString = "";
            int i = 0;
            for (PatternElem elem : pattern) {
                if (elem == PatternElem.CNT) {
                    resultString += words[i] + " ";
                }
                ++i;
            }
            resultString = resultString.replaceFirst(" $", "");
            return resultString;
        } else {
            return "";
        }
    }

}
