package de.typology.evaluation;

import de.typology.predictors.LuceneNGramSearcher;

public class NGramEvaluator {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int joinLength = 100;
		int topK = 10;
		LuceneNGramSearcher lns = new LuceneNGramSearcher(2, topK, joinLength);
		// for (int joinLength = 5; joinLength < 50; joinLength = joinLength +
		// 2) {
		for (int n = 5; n > 1; n--) {
			lns.setTestParameter(n, topK, joinLength);
			lns.run();
			// }
		}

		// LuceneNGramSearcher lns = new LuceneNGramSearcher(5, 10, 12);

	}
}
