package de.typology.googleNGrams;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import de.typology.utils.Config;

public class NGramDownloadBuilder {

	/**
	 * output example:
	 * http://commondatastorage.googleapis.com/books/ngrams/books
	 * /googlebooks-ger-all-5gram-20090715-0.csv.zip
	 * <p>
	 * ...
	 * <p>
	 * http://commondatastorage
	 * .googleapis.com/books/ngrams/books/googlebooks-ger
	 * -all-5gram-20090715-799.csv.zip
	 * 
	 * @param args
	 * @throws IOException
	 * @author Martin Koerner
	 */
	public static void main(String[] args) throws IOException {
		// example for a link:
		// http://commondatastorage.googleapis.com/books/ngrams/books/googlebooks-ger-all-5gram-20090715-799.csv.zip
		String linkPart1 = "http://commondatastorage.googleapis.com/books/ngrams/books/";
		String linkPart2 = Config.get().ngramDownloadPath;

		Writer writer = new OutputStreamWriter(new FileOutputStream(
				Config.get().ngramDownloadOutputPath + linkPart2 + ".sh"));
		for (int i = 0; i < 800; i++) {
			writer.write("wget " + linkPart1 + linkPart2 + "-" + i + ".csv.zip");
			writer.write("\n");
		}
		writer.flush();
		writer.close();
	}
}
