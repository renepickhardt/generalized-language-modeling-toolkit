package de.typology.executables;

import java.io.File;
import java.io.IOException;

import de.typology.lexerParser.DataSetSplitter;
import de.typology.lexerParser.WikipediaMain;
import de.typology.nGramBuilder.NGramBuilder;
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
		String parsedOutputPath;
		String normalizedOutputPath;
		// parse and normalize wikipedia data:

		File dir = new File(Config.get().wikiInputDirectory);
		String outputDirectory = Config.get().outputDirectory + "wiki/";
		new File(outputDirectory).mkdirs();
		for (File f : dir.listFiles()) {
			// PARSE WIKI!
			String wikiTyp = f.getName().split("-")[0];
			String outputPath = outputDirectory + wikiTyp + "/";
			new File(outputPath).mkdirs();
			parsedOutputPath = outputPath + "parsed.txt";
			normalizedOutputPath = outputPath + "normalized.txt";

			if (Config.get().parseData) {
				WikipediaMain.run(f.getAbsolutePath(), parsedOutputPath,
						normalizedOutputPath);
			}

			if (Config.get().sampleSplitData) {
				splitAndTrain(outputPath, normalizedOutputPath);
			}

			//
			// TypologyEvaluator.main(args);
			//
			// NGramEvaluator.main(args);

		}
	}

	/**
	 * 
	 * @param outputPath
	 *            path where splitted files and trained ngrams as well as
	 *            indices will be stored
	 * @param fileToBeSplit
	 *            path and filename of file that needs to be split e.g. parsed
	 *            rawdata
	 * @throws IOException
	 */
	public static void splitAndTrain(String outputPath, String fileToBeSplit)
			throws IOException {
		// DATA SPLIT create paths and direcotries for training and test
		// data
		String ratePathSuffix = "Sam" + Config.get().sampleRate + "Split"
				+ Config.get().splitDataRatio + "Test"
				+ Config.get().splitTestRatio;
		String testPath = outputPath + "test" + ratePathSuffix + "/";
		String trainingPath = outputPath + "training" + ratePathSuffix + "/";
		String learningPath = outputPath + "learning" + ratePathSuffix + "/";
		new File(trainingPath).mkdirs();
		new File(testPath).mkdirs();
		new File(learningPath).mkdirs();

		String testFile = testPath + "test.file";
		String trainingFile = trainingPath + "training.file";
		String learningFile = learningPath + "learning.file";

		if (Config.get().sampleSplitData) {
			DataSetSplitter.run(fileToBeSplit, testFile, trainingFile,
					learningFile);
		}

		NGramBuilder.run(trainingPath, trainingFile);

		// String normalizedEdges = trainingPath
		// + Config.get().typologyEdgesPathNotAggregated + "Normalized/";
		// String indexEdges = trainingPath
		// + Config.get().typologyEdgesPathNotAggregated + "Index/";
		// LuceneTypologyIndexer.run(normalizedEdges, indexEdges);
		//
		// String normalizedNGrams = trainingPath
		// + Config.get().nGramsNotAggregatedPath + "Normalized/";
		// String indexNGrams = trainingPath
		// + Config.get().nGramsNotAggregatedPath + "Index/";
		// LuceneNGramIndexer.run(normalizedNGrams, indexNGrams);
	}

}
