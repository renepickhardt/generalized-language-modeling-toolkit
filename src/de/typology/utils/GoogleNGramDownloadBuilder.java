package de.typology.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class GoogleNGramDownloadBuilder {

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
		String linkPart3 = "gram-20090715";

		for (int nGramCount = 1; nGramCount < 6; nGramCount++) {
			Writer writer = new OutputStreamWriter(new FileOutputStream(
					Config.get().ngramDownloadOutputPath + linkPart2
							+ nGramCount + linkPart3 + ".sh"));
			for (int i = 0; i < 800; i++) {
				if (nGramCount == 1 && i > 9) {
					continue;
				}
				if (nGramCount == 2 && i > 99) {
					continue;
				}
				if (nGramCount == 3 && i > 199) {
					continue;
				}
				if (nGramCount == 4 && i > 399) {
					continue;
				}
				writer.write("wget " + linkPart1 + linkPart2 + nGramCount
						+ linkPart3 + "-" + i + ".csv.zip");
				writer.write("\n");
			}

			writer.flush();
			writer.close();
		}
	}
}
