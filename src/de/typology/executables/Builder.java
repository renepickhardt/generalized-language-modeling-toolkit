package de.typology.executables;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.typology.indexes.WordIndexer;
import de.typology.splitter.GLMSplitter;
import de.typology.utils.Config;

public class Builder {

	static Logger logger = LogManager.getLogger(Builder.class.getName());

	public static void main(String[] args) {

		// TODO: parameters as arguments
		File inputDirectory = new File(Config.get().outputDirectory
				+ Config.get().inputDataSet);
		File inputFile = new File(inputDirectory + "/training.txt");
		File indexFile = new File(inputDirectory + "/index.txt");
		File outputDirectory = new File(Config.get().outputDirectory
				+ Config.get().inputDataSet + "/absolute");
		if (Config.get().buildIndex) {
			logger.info("build word index: " + indexFile.getAbsolutePath());
			WordIndexer wordIndexer = new WordIndexer();
			wordIndexer.buildIndex(inputFile, indexFile,
					Config.get().maxCountDivider);
		}
		if (Config.get().buildGLM) {
			GLMSplitter glmSplitter = new GLMSplitter(inputFile, indexFile,
					outputDirectory, Config.get().maxCountDivider, "\t",
					Config.get().deleteTempFiles);
			logger.info("split into GLM sequences: "
					+ inputFile.getAbsolutePath());
			glmSplitter.splitGLMForSmoothing(Config.get().modelLength);
		}
		logger.info("done");
	}

}
