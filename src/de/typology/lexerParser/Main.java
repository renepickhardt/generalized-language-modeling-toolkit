package de.typology.lexerParser;

import java.io.File;
import java.io.IOException;

import de.typology.utils.Config;

public class Main {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		WikipediaTokenizer tokenizer = new WikipediaTokenizer(
				Config.get().wikiXmlPath);
		WikipediaRecognizer recognizer = new WikipediaRecognizer(tokenizer);
		WikipediaParser parser = new WikipediaParser(recognizer);
		System.out.println("Start parsing.");
		parser.parse();
		System.out.println("Parsing done.");
		System.out.println("Start cleanup.");
		WikipediaNormalizer wn = new WikipediaNormalizer(
				Config.get().parsedWikiOutputPath,
				Config.get().normalizedWikiOutputPath);
		wn.normalize();
		System.out.println("Cleanup done.");
		System.out.println("Generate indicator file.");
		File done = new File(Config.get().normalizedWikiOutputPath + "IsDone");
		done.createNewFile();
		System.out.println("Done.");
	}

}
