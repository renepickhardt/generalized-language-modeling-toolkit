package de.typology.executables;

import java.io.File;
import java.io.IOException;

import de.typology.lexerParser.DGTTMMain;
import de.typology.lexerParser.DataSetSplitter;
import de.typology.lexerParser.EnronMain;
import de.typology.nGramBuilder.NGramBuilder;
import de.typology.trainers.LuceneNGramIndexer;
import de.typology.trainers.LuceneTypologyIndexer;
import de.typology.utils.Config;

public class MixedNGramBuilder {
	/**
	 * executes the following steps:
	 * <p>
	 * 1) parse and normalize dgttm, enron and reuters data
	 * <p>
	 * 2) build ngrams
	 * <p>
	 * 
	 * @author Rene Pickhardt
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		String outputDirectory;
		String outputPath;
		String parsedOutputPath;
		String normalizedOutputPath;
		// parse and normalize data:

		new File(Config.get().outputDirectory + "dgttm/").mkdirs();
		outputDirectory = Config.get().outputDirectory + "dgttm/";
		String[] languages = Config.get().DGTTMLanguages.split(",");
		for (String language : languages) {
			outputPath = outputDirectory + language + "/";
			new File(outputPath).mkdirs();
			parsedOutputPath = outputPath + "parsed.txt";
			normalizedOutputPath = outputPath + "normalized.txt";
			if (Config.get().parseData) {
				DGTTMMain.run(Config.get().dgttmInputDirectory,
						parsedOutputPath, normalizedOutputPath, language);
			}
			if (Config.get().sampleSplitData) {
				splitAndTrain(outputPath, normalizedOutputPath);
			}
			//
			// TypologyEvaluator.main(args);
			//
			// NGramEvaluator.main(args);
		}

		new File(Config.get().outputDirectory + "enron/").mkdirs();
		outputDirectory = Config.get().outputDirectory + "enron/";
		new File(outputDirectory).mkdirs();
		parsedOutputPath = outputDirectory + "parsed.txt";
		normalizedOutputPath = outputDirectory + "normalized.txt";
		if (Config.get().parseData) {
			EnronMain.run(Config.get().enronInputDirectory, parsedOutputPath,
					normalizedOutputPath);
		}
		if (Config.get().sampleSplitData) {
			splitAndTrain(outputDirectory, normalizedOutputPath);
		}
		//
		// TypologyEvaluator.main(args);
		//
		// NGramEvaluator.main(args);
		//
		//		new File(Config.get().outputDirectory + "reuters/").mkdirs();
		//		outputDirectory = Config.get().outputDirectory + "reuters/";
		//		new File(outputDirectory).mkdirs();
		//		parsedOutputPath = outputDirectory + "parsed.txt";
		//		normalizedOutputPath = outputDirectory + "normalized.txt";
		//		if (Config.get().parseData) {
		//			ReutersMain.run(Config.get().reutersInputDirectory,
		//					parsedOutputPath, normalizedOutputPath);
		//		}
		//		if (Config.get().sampleSplitData) {
		//			splitAndTrain(outputDirectory, normalizedOutputPath);
		//		}
		//		//
		//		// TypologyEvaluator.main(args);
		//		//
		//		// NGramEvaluator.main(args);

	}

	public static void splitAndTrain(String outputPath,
			String fileToBeSplit) throws IOException {
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

		String normalizedEdges = trainingPath
				+ Config.get().typologyEdgesPathNotAggregated + "Normalized/";
		String indexEdges = trainingPath
				+ Config.get().typologyEdgesPathNotAggregated + "Index/";
		LuceneTypologyIndexer.run(normalizedEdges, indexEdges);

		String normalizedNGrams = trainingPath
				+ Config.get().nGramsNotAggregatedPath + "Normalized/";
		String indexNGrams = trainingPath
				+ Config.get().nGramsNotAggregatedPath + "Index/";
		LuceneNGramIndexer.run(normalizedNGrams, indexNGrams);

	}
}
