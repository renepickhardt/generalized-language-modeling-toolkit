package de.typology.parser;

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
		// TODO: add traversing through directory
		WikipediaTokenizer tokenizer = new WikipediaTokenizer(
				Config.get().wikiInputDirectory
						+ "dewiki-20130219-pages-articles.xml.bz2");
		WikipediaRecognizer recognizer = new WikipediaRecognizer(tokenizer);
		WikipediaLinkExtractor linkExtractor = new WikipediaLinkExtractor(
				recognizer, Config.get().wikiLinksOutputPath,
				Config.get().wikiLinksHead);
		// head could be something like "en:wiki:"
		System.out.println("start extracting");
		linkExtractor.extract();
		System.out.println("extracting done");
		System.out.println("generate indicator file");
		File done = new File(Config.get().wikiLinksOutputPath + "IsDone");
		done.createNewFile();
		System.out.println("done");
	}

}
