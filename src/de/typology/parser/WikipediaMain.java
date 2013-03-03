package de.typology.parser;

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
		File dir = new File(Config.get().wikiInputDirectory);
		String outputDirectory = Config.get().outputDirectory + "wiki/";
		new File(outputDirectory).mkdirs();
		//
		// run("/home/martin/typology/datasets/wiki/barwiki-20130227-pages-articles.xml.bz2",
		// Config.get().outputDirectory + "barwiki/parsed.txt",
		// Config.get().outputDirectory + "barwiki/normalized.txt");

		run("/home/martin/typology/datasets/wiki/enwiki-20130204-pages-articles.xml.bz2",
				Config.get().outputDirectory + "enwiki/parsed.txt",
				Config.get().outputDirectory + "enwiki/normalized.txt");
	}

	public static void run(String wikiInputPath, String parsedOutputPath,
			String normalizedOutputPath) throws IOException {

		long startTime = System.currentTimeMillis();
		WikipediaTokenizer tokenizer = new WikipediaTokenizer(wikiInputPath);
		WikipediaRecognizer recognizer = new WikipediaRecognizer(tokenizer);
		WikipediaParser parser = new WikipediaParser(recognizer,
				parsedOutputPath);
		System.out.println("start parsing");
		parser.parse();
		System.out.println("parsing done");
		System.out.println("start cleanup");
		WikipediaNormalizer wn = new WikipediaNormalizer(parsedOutputPath,
				normalizedOutputPath);
		wn.normalize();
		System.out.println("cleanup done");
		System.out.println("generate indicator file");
		long endTime = System.currentTimeMillis();
		long time = (endTime - startTime) / 1000;
		File done = new File(normalizedOutputPath + "IsDone." + time + "s");
		done.createNewFile();
		System.out.println("done");

	}

}
