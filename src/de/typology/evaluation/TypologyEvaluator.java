package de.typology.evaluation;

import de.typology.predictors.LuceneTypologySearcher;

public class TypologyEvaluator extends Evaluator {
	public static void main(String[] args) {
		LuceneTypologySearcher lts = new LuceneTypologySearcher();
		int joinLength = 100;
		int topK = 10;
		// for (int joinLength = 5; joinLength < 50; joinLength = joinLength +
		// 2) {
		for (int n = 5; n < 6; n++) {
			lts.run(lts, n, topK, joinLength);
			// }
		}
	}
}
