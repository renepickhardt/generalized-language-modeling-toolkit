package de.typology.executables;

import java.io.File;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.typology.evaluators.EntropyEvaluator;
import de.typology.indexes.WordIndex;
import de.typology.indexes.WordIndexer;
import de.typology.patterns.PatternBuilder;
import de.typology.smoother.KneserNeySmoother;
import de.typology.splitter.AbsoluteSplitter;
import de.typology.splitter.DataSetSplitter;
import de.typology.splitter.SmoothingSplitter;
import de.typology.tester.TestSequenceExtractor;
import de.typology.utils.Config;

public class KneserNeyBuilder {

	static Logger logger = LogManager.getLogger(KneserNeyBuilder.class
			.getName());

	public static void main(String[] args) {

		// TODO: parameters as arguments
		File inputDirectory = new File(Config.get().outputDirectory
				+ Config.get().inputDataSet);
		File inputFile = new File(inputDirectory.getAbsolutePath()
				+ "/training.txt");
		File indexFile = new File(inputDirectory.getAbsolutePath()
				+ "/index.txt");
		File absoluteDirectory = new File(inputDirectory.getAbsolutePath()
				+ "/absolute");
		File continuationDirectory = new File(inputDirectory.getAbsolutePath()
				+ "/continuation");
		if (Config.get().splitData) {
			DataSetSplitter dss = new DataSetSplitter(inputDirectory,
					"normalized.txt");
			dss.split("training.txt", "learning.txt", "testing.txt",
					Config.get().modelLength);
			dss.splitIntoSequences(new File(inputDirectory.getAbsolutePath()
					+ "/testing.txt"), Config.get().modelLength,
					Config.get().numberOfQueries);
			// dss.splitIntoSequences(new
			// File(inputDirectory.getAbsolutePath()+"/learning.txt"),
			// Config.get().modelLength,Config.get().numberOfQueries);
			// dss.splitIntoSequences(new
			// File(inputDirectory.getAbsolutePath()+"/testing.txt"),
			// Config.get().modelLength,Config.get().numberOfQueries);
		}
		if (Config.get().buildIndex) {
			logger.info("build word index: " + indexFile.getAbsolutePath());
			WordIndexer wordIndexer = new WordIndexer();
			wordIndexer.buildIndex(inputFile, indexFile,
					Config.get().maxCountDivider, "<fs> <s> ", " </s>");
		}
		if (Config.get().buildGLM) {
			ArrayList<boolean[]> glmForSmoothingPatterns = PatternBuilder
					.getReverseGLMForSmoothingPatterns(Config.get().modelLength);
			AbsoluteSplitter absolteSplitter = new AbsoluteSplitter(inputFile,
					indexFile, absoluteDirectory, "\t",
					Config.get().deleteTempFiles, "<fs> <s> ", " </s>");
			logger.info("split into GLM sequences: "
					+ inputFile.getAbsolutePath());
			absolteSplitter.split(glmForSmoothingPatterns,
					Config.get().numberOfCores);
		}
		if (Config.get().buildContinuationGLM) {
			// ArrayList<boolean[]> glmForSmoothingPatterns = PatternBuilder
			// .getReverseGLMForSmoothingPatterns(Config.get().modelLength);
			// OldSmoothingSplitter oldSmoothingSplitter = new
			// OldSmoothingSplitter(
			// absoluteDirectory, indexFile, Config.get().maxCountDivider,
			// "\t", Config.get().deleteTempFiles);
			//
			// logger.info("split into old continuation sequences: "
			// + inputFile.getAbsolutePath());
			// oldSmoothingSplitter.split(glmForSmoothingPatterns,
			// Config.get().numberOfCores);
			ArrayList<boolean[]> lmPatterns = PatternBuilder
					.getReverseLMPatterns(Config.get().modelLength);
			SmoothingSplitter smoothingSplitter = new SmoothingSplitter(
					absoluteDirectory, continuationDirectory, indexFile, "\t",
					Config.get().deleteTempFiles);
			logger.info("split into continuation sequences: "
					+ inputFile.getAbsolutePath());
			smoothingSplitter.split(lmPatterns, Config.get().numberOfCores);
		}

		File testExtractOutputDirectory = new File(
				inputDirectory.getAbsolutePath() + "/testing-samples");
		if (Config.get().extractContinuationGLM) {
			File testSequences = new File(inputDirectory.getAbsolutePath()
					+ "/testing-samples-" + Config.get().modelLength + ".txt");
			testExtractOutputDirectory.mkdir();

			TestSequenceExtractor tse = new TestSequenceExtractor(
					testSequences, absoluteDirectory, continuationDirectory,
					testExtractOutputDirectory, "\t", new WordIndex(indexFile));
			tse.extractSequences(Config.get().modelLength,
					Config.get().numberOfCores);
			tse.extractContinuationSequences(Config.get().modelLength,
					Config.get().numberOfCores);

		}

		if (Config.get().buildKneserNey) {
			KneserNeySmoother kns = new KneserNeySmoother(
					testExtractOutputDirectory, absoluteDirectory,
					continuationDirectory, "\t", Config.get().decimalPlaces);
			// TODO remove comments
			// for (int i = 1; i <= Config.get().modelLength; i++) {
			for (int i = 5; i <= Config.get().modelLength; i++) {
				File inputSequenceFile = new File(
						inputDirectory.getAbsolutePath() + "/testing-samples-"
								+ i + ".txt");
				File resultFile;
				// smooth simple
				if (Config.get().kneserNeySimple) {
					resultFile = new File(inputDirectory.getAbsolutePath()
							+ "/kneser-ney-simple-" + i + ".txt");
					kns.smooth(inputSequenceFile, resultFile, i, false);

				}
				// // smooth complex
				if (Config.get().kneserNeyComplex) {
					resultFile = new File(inputDirectory.getAbsolutePath()
							+ "/kneser-ney-complex-" + i + ".txt");
					kns.smooth(inputSequenceFile, resultFile, i, true);
				}
			}
		}
		if (Config.get().calculateEntropy) {
			EntropyEvaluator ee = new EntropyEvaluator();
			// TODO remove comments
			// for (int i = 1; i <= Config.get().modelLength; i++) {
			for (int i = 5; i <= Config.get().modelLength; i++) {
				File inputSequenceFile = new File(
						inputDirectory.getAbsolutePath() + "/testing-samples-"
								+ i + ".txt");
				File resultFile;
				// smooth simple
				if (Config.get().kneserNeySimple) {
					resultFile = new File(inputDirectory.getAbsolutePath()
							+ "/kneser-ney-simple-" + i + ".txt");

					File entropyResultFile = new File(
							inputDirectory.getAbsolutePath() + "/entropy-"
									+ resultFile.getName());
					ee.caculateEntropy(inputSequenceFile, resultFile,
							entropyResultFile, "\t");

				}
				// // smooth complex
				if (Config.get().kneserNeyComplex) {
					resultFile = new File(inputDirectory.getAbsolutePath()
							+ "/kneser-ney-complex-" + i + ".txt");
					File entropyResultFile = new File(
							inputDirectory.getAbsolutePath() + "/entropy-"
									+ resultFile.getName());
					ee.caculateEntropy(inputSequenceFile, resultFile,
							entropyResultFile, "\t");
				}
			}
		}

		// File _absoluteDirecory = new File(inputDirectory.getAbsolutePath()
		// + "/_absolute");
		// File _absolute_Direcory = new File(inputDirectory.getAbsolutePath()
		// + "/_absolute_");
		// File absolute_Direcory = new File(inputDirectory.getAbsolutePath()
		// + "/absolute_");
		// if (Config.get().buildKneserNey) {
		// File kneserNeyOutputDirectory = new File(
		// inputDirectory.getAbsolutePath() + "/kneser-ney");
		// KneserNeySmoother kns = new KneserNeySmoother(
		// absoluteOutputDirectory, _absoluteDirecory,
		// _absolute_Direcory, absolute_Direcory,
		// kneserNeyOutputDirectory, new WordIndex(indexFile), "\t",
		// Config.get().decimalPlaces, Config.get().deleteTempFiles);
		// kns.deleteResults();
		// for (int i = 1; i <= Config.get().modelLength; i++) {
		// // call KneserNeySmoother
		// kns.smoothComplex(i, Config.get().numberOfCores);
		// }
		// }
		logger.info("done");
	}
}
