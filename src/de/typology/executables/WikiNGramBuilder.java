package de.typology.executables;

import java.io.IOException;

import de.typology.lexerParser.WikipediaMain;
import de.typology.nGramBuilder.BuildNGrams;

public class WikiNGramBuilder {

	/**
	 * executes the following steps:
	 * <p>
	 * 1) parse and normalize wikipedia data
	 * <p>
	 * 2) build ngrams
	 * <p>
	 * 
	 * @author Martin Koerner
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		// parse and normalize wikipedia data:
		WikipediaMain.main(args);

		// build ngrams:
		BuildNGrams.main(args);

	}

}
