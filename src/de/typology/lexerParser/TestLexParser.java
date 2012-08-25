package de.typology.lexerParser;

import java.io.IOException;

import de.typology.utils.Config;

public class TestLexParser {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		// wiki tests
		//

		// test parser
		//
		WikipediaTokenizer tokenizer = new WikipediaTokenizer(
				Config.get().wikiXmlPath);
		WikipediaRecognizer recognizer = new WikipediaRecognizer(tokenizer);
		WikipediaParser parser = new WikipediaParser(recognizer);

		parser.parse();

		// test recognizer
		//
		// for (int i = 0; i < 4000; i++) {
		// // while (tokenizer.hasNext()) {
		// if (recognizer.hasNext()) {
		// WikipediaToken t = recognizer.next();
		// System.out.println(t + " : " + recognizer.getLexeme());
		// }
		// }

		// test tokenizer
		//
		// for (int i = 0; i < 2000; i++) {
		// while (tokenizer.hasNext()) {
		// if (tokenizer.hasNext()) {
		// WikipediaToken t = tokenizer.next();
		// System.out.println(t + " : " + tokenizer.getLexeme());
		// }
		// }

		// test WikipediaNormalizer
		//
		WikipediaNormalizer wn = new WikipediaNormalizer(
				Config.get().parsedWikiOutputPath,
				Config.get().normalizedWikiOutputPath);
		wn.normalize();
		//
		//
		// reuters tests
		//
		// File dir = new File(Config.get().reutersXmlPath);
		// File[] fileList = dir.listFiles();
		// for (File f : fileList) {
		// ReutersRecognizer recognizer = new ReutersRecognizer(f);
		// while (recognizer.hasNext()) {
		// ReutersToken t = recognizer.next();
		// System.out.println(t + " : " + recognizer.getLexeme());
		// }
		// }
		//
		// ReutersRecognizer recognizer = new ReutersRecognizer(new File(
		// Config.get().reutersXmlPath + "100011newsML.xml"));
		// while (recognizer.hasNext()) {
		// ReutersToken t = recognizer.next();
		// System.out.println(t + " : " + recognizer.getLexeme());
		// }

		// enron tests
		//
		// EnronRecognizer recognizer = new EnronRecognizer(new File(
		// Config.get().enronPath + "154"));
		//
		// while (recognizer.hasNext()) {
		// EnronToken t = recognizer.next();
		// System.out.println(t + " : " + recognizer.getLexeme());
		// }

		// DGT-TM tests
		//
		// DGTTMRecognizer recognizer = new DGTTMRecognizer(new
		// File(Config.get().DGTTMPath
		// + "\\Vol_2004_1\\22004D0069.tmx"));
		// int i = 0;
		// // while (recognizer.hasNext()) {
		// while (i < 2000) {
		// DGTTMToken t = recognizer.next();
		// System.out.println(t + " : " + recognizer.getLexeme());
		// i++;
		// }
		// string tests
		//
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
