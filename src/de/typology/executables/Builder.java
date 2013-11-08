package de.typology.executables;

import java.io.File;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.typology.indexes.WordIndexer;
import de.typology.patterns.PatternBuilder;
import de.typology.splitter.AbsoluteSplitter;
import de.typology.splitter.SmoothingSplitter;
import de.typology.utils.Config;

public class Builder {

	static Logger logger = LogManager.getLogger(Builder.class.getName());

	public static void main(String[] args) {

		// TODO: parameters as arguments
		File inputDirectory = new File(Config.get().outputDirectory
				+ Config.get().inputDataSet);
		File inputFile = new File(inputDirectory + "/training.txt");
		File indexFile = new File(inputDirectory + "/index.txt");
		File absoluteOutputDirectory = new File(Config.get().outputDirectory
				+ Config.get().inputDataSet + "/absolute");
		if (Config.get().buildIndex) {
			logger.info("build word index: " + indexFile.getAbsolutePath());
			WordIndexer wordIndexer = new WordIndexer();
			wordIndexer.buildIndex(inputFile, indexFile,
					Config.get().maxCountDivider);
		}
		if (Config.get().buildGLM) {
			ArrayList<boolean[]> glmForSmoothingPatterns = PatternBuilder
					.splitGLMForSmoothing(Config.get().modelLength);
			AbsoluteSplitter absolteSplitter = new AbsoluteSplitter(inputFile,
					indexFile, absoluteOutputDirectory,
					Config.get().maxCountDivider, "\t",
					Config.get().deleteTempFiles);
			logger.info("split into GLM sequences: "
					+ inputFile.getAbsolutePath());
			absolteSplitter.split(glmForSmoothingPatterns);
		}
		if (Config.get().build_absoluteGLM) {
			ArrayList<boolean[]> glmForSmoothingPatterns = PatternBuilder
					.splitGLMForSmoothing(Config.get().modelLength);
			SmoothingSplitter smoothingSplitter = new SmoothingSplitter(
					absoluteOutputDirectory, indexFile,
					Config.get().maxCountDivider, "\t",
					Config.get().deleteTempFiles);

			logger.info("split into GLM sequences: "
					+ inputFile.getAbsolutePath());
			smoothingSplitter.split(glmForSmoothingPatterns);
		}
		logger.info("done");
	}
}
