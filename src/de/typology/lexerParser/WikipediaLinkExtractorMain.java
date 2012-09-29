package de.typology.lexerParser;

import java.io.File;
import java.io.IOException;

import de.typology.utils.Config;

public class WikipediaLinkExtractorMain {

	/**
	 * @author Martin Koerner
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		WikipediaTokenizer tokenizer = new WikipediaTokenizer(
				Config.get().wikiXmlPath);
		WikipediaRecognizer recognizer = new WikipediaRecognizer(tokenizer);
		WikipediaLinkExtractor linkExtractor = new WikipediaLinkExtractor(
				recognizer, Config.get().wikiLinksOutputPath, "de:wiki:");
		System.out.println("start extracting");
		linkExtractor.extract();
		System.out.println("extracting done");
		System.out.println("generate indicator file");
		File done = new File(Config.get().wikiLinksOutputPath + "IsDone");
		done.createNewFile();
		System.out.println("done");
	}

}
