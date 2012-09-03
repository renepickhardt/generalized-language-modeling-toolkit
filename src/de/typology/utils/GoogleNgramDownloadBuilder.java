package de.typology.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class GoogleNgramDownloadBuilder {

	/**
	 * @author Martin Koerner
	 * 
	 * @param args
	 * @throws IOException
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
