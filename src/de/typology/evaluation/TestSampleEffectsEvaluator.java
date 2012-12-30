package de.typology.evaluation;

import java.io.IOException;

import de.typology.executables.WikiNGramBuilder;
import de.typology.utils.Config;

public class TestSampleEffectsEvaluator {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		part1PrepareIndices();
		part2runTests();
	}

	private static void part1PrepareIndices() {
		for (int sampleRate = 95; sampleRate > 0; sampleRate -= 5) {
			Config.get().sampleRate = sampleRate;
			Config.get().sampleSplitData = true;
			try {
				String parsedEnglishWiki = Config.get().outputDirectory
						+ "wiki/enwiki/normalized.txt";
				String outputDirectory = Config.get().outputDirectory
						+ "wiki/enwiki/";
				WikiNGramBuilder.splitAndTrain(outputDirectory,
						parsedEnglishWiki);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private static void part2runTests() {
		// TODO Auto-generated method stub

	}

}
