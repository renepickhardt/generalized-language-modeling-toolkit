package de.typology.lexerParser;

import java.io.File;
import java.io.IOException;

import de.typology.utils.Config;

public class WikipediaMain {

	/**
	 * @author Martin Koerner
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		run(Config.get().wikiXmlPath, Config.get().parsedWikiOutputPath,
				Config.get().normalizedWikiOutputPath);
	}

	public static void run(String wikiXmlPath, String parsedWikiOutputPath,
			String normalizedWikiOutputPath) throws IOException {

		long startTime = System.currentTimeMillis();
		WikipediaTokenizer tokenizer = new WikipediaTokenizer(wikiXmlPath);
		WikipediaRecognizer recognizer = new WikipediaRecognizer(tokenizer);
		WikipediaParser parser = new WikipediaParser(recognizer,
				parsedWikiOutputPath);
		System.out.println("start parsing");
		parser.parse();
		System.out.println("parsing done");
		System.out.println("start cleanup");
		WikipediaNormalizer wn = new WikipediaNormalizer(parsedWikiOutputPath,
				normalizedWikiOutputPath);
		wn.normalize();
		System.out.println("cleanup done");
		System.out.println("generate indicator file");
		long endTime = System.currentTimeMillis();
		long time = (endTime - startTime) / 1000;
		File done = new File(normalizedWikiOutputPath + "IsDone." + time + "s");
		done.createNewFile();
		System.out.println("done");

	}

}
