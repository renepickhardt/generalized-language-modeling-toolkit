package de.typology.lexerParser;

import java.io.FileNotFoundException;

import de.typology.utils.Config;

public class TestLexParser {

	/**
	 * @param args
	 * @throws FileNotFoundException
	 */
	public static void main(String[] args) throws FileNotFoundException {
		WikipediaRecognizer recognizer = new WikipediaRecognizer(
				Config.get().wikiXmlPath);

		for (int i = 0; i < 100; i++) {
			WikipediaToken t = recognizer.next();
			System.out.println(t + " : " + recognizer.getLexeme());
		}

		// String s = "[[l|]]";
		// String[] splitLabel = s.split("\\|");
		//
		// System.out.println(splitLabel[1].substring(0,
		// splitLabel[1].length() - 2));

		// String[] splitLabel = recognizer.getLexeme().split(
		// "\\|");
		// writer.write(splitLabel[1].substring(0,
		// splitLabel[1].length() - 2));

	}
}
