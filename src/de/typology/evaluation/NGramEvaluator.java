package de.typology.evaluation;

import de.typology.predictors.LuceneNGramSearcher;

public class NGramEvaluator {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int joinLength = 100;
		int topK = 10;
		LuceneNGramSearcher lns = new LuceneNGramSearcher(2, 10, 12);
		// for (int joinLength = 5; joinLength < 50; joinLength = joinLength +
		// 2) {
		for (int n = 2; n < 6; n++) {
			lns.run(lns, n, topK, joinLength);
			// }
		}

		// LuceneNGramSearcher lns = new LuceneNGramSearcher(5, 10, 12);

	}
}
