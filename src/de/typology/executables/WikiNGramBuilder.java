package de.typology.executables;

import java.io.IOException;

import de.typology.nGramBuilder.NGramNormalizer;
import de.typology.trainers.LuceneTypologyIndexer;

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
		// WikipediaMain.main(args);

		// build ngrams:
		// nGramBuilder.main(args);

		// normalize typology edges
		NGramNormalizer.main(args);

		// Put normalized edges to Lucene index:
		LuceneTypologyIndexer.main(args);
	}

}
