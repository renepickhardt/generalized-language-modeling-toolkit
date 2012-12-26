package de.typology.executables;

import java.io.IOException;

import de.typology.trainers.LuceneNGramIndexer;

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

		// DataSetSplitter.main(args);

		// build ngrams:
		// NGramBuilder.main(args);

		// normalize typology edges
		// EdgeNormalizer.main(args);

		// Put normalized edges to Lucene index:
		// LuceneTypologyIndexer.main(args);

		LuceneNGramIndexer.main(args);
		// normalize ngrams calculate maximal likelihood estimation
		// NGramNormalizer.main(args);

	}

}
