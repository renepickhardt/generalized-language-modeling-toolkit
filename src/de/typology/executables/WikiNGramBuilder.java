package de.typology.executables;

import java.io.File;
import java.io.IOException;

import de.typology.lexerParser.DataSetSplitter;
import de.typology.lexerParser.WikipediaMain;
import de.typology.nGramBuilder.NGramBuilder;
import de.typology.trainers.LuceneNGramIndexer;
import de.typology.trainers.LuceneTypologyIndexer;
import de.typology.utils.Config;

public class WikiNGramBuilder {

	/**
	 * executes the following steps:
	 * <p>
	 * 1) parse and normalize wikipedia data
	 * <p>
	 * 2) build ngrams
	 * <p>
	 * 
	 * @author Rene Pickhardt
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		// parse and normalize wikipedia data:

		File dir = new File(Config.get().wikiInputDirectory);
		new File(Config.get().outputDirectory).mkdirs();
		for (File f : dir.listFiles()) {
			// PARSE WIKI!
			String wikiTyp = f.getName().split("-")[0];
			String outPath = Config.get().outputDirectory + wikiTyp + "/";
			new File(outPath).mkdirs();
			String parsedWiki = outPath + "parsed.txt";
			String normalizedWiki = outPath + "normalized.txt";

			if (Config.get().parseWiki) {
				WikipediaMain.run(f.getAbsolutePath(), parsedWiki,
						normalizedWiki);
			}

			// DATA SPLIT create paths and direcotries for training and test
			// data
			String ratePathSuffix = "Sam" + Config.get().sampleRate + "Split"
					+ Config.get().splitDataRatio;
			String testPath = outPath + "test" + ratePathSuffix + "/";
			String trainingPath = outPath + "training" + ratePathSuffix + "/";
			new File(trainingPath).mkdirs();
			new File(testPath).mkdirs();

			String testFile = testPath + "test.file";
			String trainingFile = trainingPath + "training.file";

			if (Config.get().sampleSplitData) {
				DataSetSplitter.run(normalizedWiki, testFile, trainingFile);

			}

			NGramBuilder.run(trainingPath, trainingFile);

			String normalizedEdges = trainingPath
					+ Config.get().typologyEdgesPathNotAggregated
					+ "Normalized/";
			String indexEdges = trainingPath
					+ Config.get().typologyEdgesPathNotAggregated + "Index/";
			LuceneTypologyIndexer.run(normalizedEdges, indexEdges);

			String normalizedNGrams = trainingPath
					+ Config.get().nGramsNotAggregatedPath + "Normalized/";
			String indexNGrams = trainingPath
					+ Config.get().nGramsNotAggregatedPath + "Index/";
			LuceneNGramIndexer.run(normalizedNGrams, indexNGrams);
			//
			// TypologyEvaluator.main(args);
			//
			// NGramEvaluator.main(args);

		}
	}
}
