package de.typology.parser;

import java.util.Stack;

/**
 * @author Rene Pickhardt, Till Speicher
 * 
 */
public class ParserModifyText {
	/**
	 * Removes numbers from any given input text
	 * 
	 * @param inputText
	 * @return Text without numbers
	 * @author Rene Pickhardt, Till Speicher
	 */
	public static String removeNumbers(String inputText) {
		// TODO: see tests. this method does not really handle special cases
		// well
		return inputText.replaceAll("[0-9]", "");
	}

	/**
	 * Removes any whitespaces in a text longer that one space.
	 * 
	 * @param inputText
	 * @return text that contains at most one space in a row
	 * @author Rene Pickhardt and
	 *         http://stackoverflow.com/questions/2932392/java
	 *         -how-to-replace-2-or
	 *         -more-spaces-with-single-space-in-string-and-delete-lead
	 */
	public static String removeMultipleWhitespaces(String inputText) {
		inputText = inputText.trim().replaceAll(" +", " ");
		return inputText;
	}

	/**
	 * Removes all substrings in a given input text that contain only one
	 * character Pay attantion to use this function for languages where words
	 * can consist of one character
	 * 
	 * @param inputText
	 * @return text with tokens that contain at least 2 characters
	 * @author Rene Pickhardt, Till Speicher
	 */
	public static String removeSingleCharacters(String inputText) {
		// TODO: problems with single characters at the beginning and end of an
		// sentence
		inputText = inputText.replaceAll(" .( .)*[ \n]", " ");
		return inputText;
	}

	/**
	 * Removes xml childs or other stuff from a text. Method is in bad
	 * complexity class and runtime can be improved by alot
	 * 
	 * @param text
	 *            - in which to search for
	 * @param open
	 *            - tag that opens e.g. <ref>
	 * @param close
	 *            - tag that closes e.g. </ref>
	 * @return text exclouding all valid childNodes
	 * @author Rene Pickhardt
	 */
	public static String removeChildNodes(String text, String open, String close) {
		Stack<Integer> stack = new Stack<Integer>();
		for (int i = 0; i < text.length(); i++) {
			String substr = (String) text.subSequence(i, text.length());
			if (substr.startsWith(open)) {
				stack.push(i);
			}
			if (substr.startsWith(close)) {
				if (stack.isEmpty()) {
					continue;
				}
				int startPos = stack.pop();
				if (stack.isEmpty()) {
					String begin = text.substring(0, startPos);
					String end = text.substring(i + close.length(),
							text.length());
					text = begin + end;
					i = startPos;
				}
			}
		}
		return text;
	}
}
