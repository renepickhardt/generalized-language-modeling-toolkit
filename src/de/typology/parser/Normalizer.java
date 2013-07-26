package de.typology.parser;

import java.text.BreakIterator;
import java.util.Locale;

import de.typology.utils.Config;

public class Normalizer {
	protected Locale locale;

	public Normalizer(Locale locale) {
		this.locale = locale;
	}

	protected String normalizeString(String input) {
		String[] punctuationRegExes = { ",", ";", ":", "\\.", "!", "\\?" };
		// split sentences
		input = this.splitSentences(input, this.locale);

		// remove some unwanted signs
		input = input.replaceAll("\\?", "");
		input = input.replaceAll("-", "");
		input = input.replaceAll(" '", "");
		input = input.replaceAll("' ", "");

		for (String PunctuationRegEx : punctuationRegExes) {
			if (Config.get().splitPunctuation) {
				input = this.addLeadingSpace(input, PunctuationRegEx);
				input = this.addFollowingSpace(input, PunctuationRegEx);
			} else {
				input = this.removeLeadingSpace(input, PunctuationRegEx);
			}

			input = this.removeLeadingSpace(input, " ");
			input = input.replaceAll("^ +", "");
			input = input.replaceAll("^\n", "");
		}
		return input;
	}

	protected String addLeadingSpace(String input, String inputRegEx) {
		return input.replaceAll(" *" + inputRegEx, " " + inputRegEx);
	}

	protected String addFollowingSpace(String input, String inputRegEx) {
		return input.replaceAll("(" + inputRegEx + ")(\\S)", "$1 $2");
	}

	protected String removeLeadingSpace(String input, String inputRegEx) {
		return input.replaceAll(" +" + inputRegEx, inputRegEx);
	}

	// derived from
	// http://stackoverflow.com/questions/2687012/split-string-into-sentences-based-on-periods
	protected String splitSentences(String input, Locale locale) {
		if (input.isEmpty()) {
			return input;
		}
		BreakIterator iterator = BreakIterator.getSentenceInstance(locale);
		iterator.setText(input);
		int start = iterator.first();
		String output = "";
		for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator
				.next()) {
			output += input.substring(start, end) + "\n";
		}
		return output.replaceAll(" +\n", "\n");
	}
}
