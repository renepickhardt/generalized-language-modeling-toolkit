package de.typology.executables;

import de.typology.smoother.Absolute_Aggregator;
import de.typology.smoother._absoluteSplitter;
import de.typology.smoother._absolute_Aggregator;
import de.typology.splitter.DataSetSplitter;
import de.typology.splitter.GLMSplitter;
import de.typology.splitter.IndexBuilder;
import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public class Builder {

	/**
	 * Takes a normalized input file from Config.get().trainingName and creates
	 * Generalized Language Models which also include standard Language Models
	 * 
	 * @param inputDirectory
	 */
	public void build(String inputDirectory) {

		String learningFileName = "learning.txt";
		String testingFileName = "testing.txt";

		if (Config.get().splitData) {
			DataSetSplitter dss = new DataSetSplitter(inputDirectory,
					"normalized.txt");
			dss.split(Config.get().trainingName, learningFileName,
					testingFileName, Config.get().modelLength);
			dss.splitIntoSequences(learningFileName, Config.get().modelLength);
			dss.splitIntoSequences(testingFileName, Config.get().modelLength);
		}
		// TODO: add stats

		if (Config.get().buildIndex) {
			IndexBuilder ib = new IndexBuilder();
			ib.buildIndex(inputDirectory + Config.get().trainingName,
					inputDirectory + Config.get().indexName, inputDirectory
							+ Config.get().statsName);
		}

		if (Config.get().buildNGrams) {
			// TODO: add GLMSplitter with Ngramoptions
		}
		if (Config.get().buildTypoEdges) {
			// TODO: add GLMSplitter with Typooptions
		}
		if (Config.get().buildGLM) {
			GLMSplitter glms = new GLMSplitter(inputDirectory);
			glms.splitGLMForKneserNey(Config.get().modelLength);
		}
		if (Config.get().build_absoluteGLM) {
			_absoluteSplitter _absoluteSplitter = new _absoluteSplitter(
					inputDirectory, "absolute", "_absolute-unaggregated",
					Config.get().indexName, Config.get().statsName,
					Config.get().trainingName, false);
			try {
				_absoluteSplitter.split(Config.get().modelLength);
			} catch (Exception e) {
				int mb = 1024 * 1024;
				// Getting the runtime reference from system
				Runtime runtime = Runtime.getRuntime();

				IOHelper.log("##### Heap utilization statistics [MB] #####");

				// Print used memory
				IOHelper.log("Used Memory:\t"
						+ (runtime.totalMemory() - runtime.freeMemory()) / mb);

				// Print free memory
				IOHelper.log("Free Memory:\t" + runtime.freeMemory() / mb);

				// Print total available memory
				IOHelper.log("Total Memory:\t" + runtime.totalMemory() / mb);

				// Print Maximum available memory
				IOHelper.log("Max Memory:\t" + runtime.maxMemory() / mb);
			}
		}

		if (Config.get().aggregateAbsolute_) {
			Absolute_Aggregator absolute_Aggregator = new Absolute_Aggregator(
					inputDirectory, "absolute", "absolute_");
			absolute_Aggregator.aggregate(Config.get().modelLength);
			_absolute_Aggregator _absolute_Aggregator = new _absolute_Aggregator(
					inputDirectory, "_absolute", "_absolute_");
			_absolute_Aggregator.aggregate(Config.get().modelLength);
		}
	}
}
