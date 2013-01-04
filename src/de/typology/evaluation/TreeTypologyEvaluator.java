package de.typology.evaluation;

import de.typology.predictors.TreeTypologySearcher;

public class TreeTypologyEvaluator extends Evaluator {
	public static void main(String[] args) {

		int joinLength = 100;
		int topK = 10;
		TreeTypologySearcher tts = new TreeTypologySearcher(5, topK, joinLength);
		// for (int joinLength = 5; joinLength < 50; joinLength = joinLength +
		// 2) {
		for (int n = 2; n < 6; n++) {
			tts.setTestParameter(n, topK, joinLength);
			tts.run();
			// }
		}
	}
}
