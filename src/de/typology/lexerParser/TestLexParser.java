package de.typology.lexerParser;

import java.io.FileNotFoundException;

public class TestLexParser {

	/**
	 * @param args
	 * @throws FileNotFoundException
	 */
	public static void main(String[] args) throws FileNotFoundException {
		Recognizer recognizer = new Recognizer(
				"D:/Arbeit/Typology/dewiki-20120630-pages-meta-current.xml");

		for (int i = 0; i < 20000; i++) {
			Token t = recognizer.next();
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
