package de.typology.executables;

import java.io.File;
import java.io.IOException;

import de.typology.googleNGrams.NGramParserMain;
import de.typology.utils.Config;

public class GoogleNGramBuilder {

	/**
	 * executes the following steps:
	 * <p>
	 * 1) parse and normalize google ngram data
	 * <p>
	 * 
	 * @author Rene Pickhardt, Martin Koerner
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		// parse and normalize google ngram data:

		File dir = new File(Config.get().googleInputDirectory);
		new File(Config.get().outputDirectory).mkdirs();
		for (File f : dir.listFiles()) {
			// PARSE NGRAMS!
			String googleTyp = f.getName();
			String outPath = Config.get().outputDirectory + "google/"
					+ googleTyp + "/";
			new File(outPath).mkdirs();
			String parsedGoogle = outPath + "parsed.txt";
			String normalizedGoogle = outPath + "normalized.txt";
			if (Config.get().parseData) {
				NGramParserMain.run(f.getAbsolutePath(), parsedGoogle,
						normalizedGoogle);
			}
		}
	}
}
