package de.typology.executables;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.HashMap;
import java.util.Map.Entry;

import de.typology.smoother.ContinuationDeltaAggregator;
import de.typology.smoother.ContinuationSplitter;
import de.typology.splitter.DataSetSplitter;
import de.typology.splitter.GLMSplitter;
import de.typology.splitter.GLMZeroBuilder;
import de.typology.splitter.IndexBuilder;
import de.typology.splitter.NGramSplitter;
import de.typology.splitter.NGramSplitterWithCount;
import de.typology.splitter.TypoSplitter;
import de.typology.splitter.TypoSplitterWithCount;
import de.typology.stats.StatsBuilder;
import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public class Builder {

	public void build(String outputPath) {

		String indexFileName = "index.txt";
		String statsFileName = "stats.txt";
		String trainingFileName = "training.txt";
		String learningFileName = "learning.txt";
		String testingFileName = "testing.txt";

		if (Config.get().splitData) {
			// index and stats are empty since they don't exist at the moment
			DataSetSplitter dss = new DataSetSplitter(outputPath, "", "",
					"normalized.txt");
			dss.split(trainingFileName, learningFileName, testingFileName,
					Config.get().modelLength);
		}
		// TODO: add stats

		if (Config.get().buildIndex) {
			IndexBuilder ib = new IndexBuilder();
			ib.buildIndex(outputPath + trainingFileName, outputPath
					+ indexFileName, outputPath + statsFileName);
		}

		if (Config.get().buildNGrams) {
			NGramSplitter ngs = new NGramSplitter(outputPath, indexFileName,
					statsFileName, trainingFileName);
			ngs.split(Config.get().modelLength);
		}
		if (Config.get().buildTypoEdges) {
			TypoSplitter ts = new TypoSplitter(outputPath, indexFileName,
					statsFileName, trainingFileName);
			ts.split(Config.get().modelLength);
		}
		if (Config.get().buildGLM) {
			GLMSplitter glms = new GLMSplitter(outputPath, indexFileName,
					statsFileName, trainingFileName);
			glms.brh = new HashMap<BufferedReader, String>();
			glms.bwh = new HashMap<BufferedWriter, String>();
			glms.splitGLM(Config.get().modelLength);
		}
		if (Config.get().buildContinuationGLM) {
			ContinuationSplitter cs = new ContinuationSplitter(outputPath,
					"index.txt", "stats.txt", "training.txt");
			cs.brh = new HashMap<BufferedReader, String>();
			cs.bwh = new HashMap<BufferedWriter, String>();
			try {
				cs.split(Config.get().modelLength);
			} catch (Exception e) {
				for (Entry<BufferedReader, String> r : cs.brh.entrySet()) {
					System.out.println(r.getValue());
				}
				for (Entry<BufferedWriter, String> r : cs.bwh.entrySet()) {
					System.out.println(r.getValue());
				}
				System.out.println("brh size: " + cs.brh.size());
				System.out.println("bwh size: " + cs.bwh.size());
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
		if (Config.get().buildZeroGLM) {
			GLMZeroBuilder glmzb = new GLMZeroBuilder(outputPath
					+ "glm-continuation/", "glm-absolute");
			glmzb.build();
		}
		if (Config.get().aggregateContinuationDelta) {
			ContinuationDeltaAggregator cda = new ContinuationDeltaAggregator(
					outputPath, "glm-aggregate");
			cda.aggregate(Config.get().modelLength);
		}
	}

	public void buildFromNGrams(String outputPath) {
		String indexFileName = "index.txt";
		String statsFileName = "stats.txt";
		String trainingFileName;
		NGramSplitterWithCount ngs;
		// no splitting into training/learning/testing since there are only
		// ngrams

		if (Config.get().buildNGrams) {
			StatsBuilder sb = new StatsBuilder();
			sb.buildStats(outputPath + "1gram-normalized.txt", outputPath
					+ "stats.txt");
		}
		// no index building; use index from another file instead

		if (Config.get().buildNGrams) {
			for (int i = 1; i <= Config.get().modelLength; i++) {
				trainingFileName = i + "gram-normalized.txt";
				ngs = new NGramSplitterWithCount(outputPath, indexFileName,
						statsFileName, trainingFileName);
				ngs.split(i);
			}
		}
		if (Config.get().buildTypoEdges) {
			for (int i = 1; i <= Config.get().modelLength; i++) {
				trainingFileName = i + "gram-normalized.txt";
				TypoSplitterWithCount ts = new TypoSplitterWithCount(
						outputPath, indexFileName, statsFileName,
						trainingFileName);
				ts.split(i);
			}
		}
	}
}
