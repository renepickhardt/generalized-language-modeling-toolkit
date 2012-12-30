package de.typology.evaluation;

import de.typology.predictors.LuceneTypologySearcher;

public class TypologyEvaluator extends Evaluator {
	public static void main(String[] args) {
		int joinLength = 100;
		int topK = 10;
		LuceneTypologySearcher lts = new LuceneTypologySearcher(5, topK,
				joinLength);
		// for (int joinLength = 5; joinLength < 50; joinLength = joinLength +
		// 2) {
		for (int n = 5; n < 6; n++) {
			lts.setTestParameter(n, topK, joinLength);
			lts.run();
			// }
		}
	}
}
