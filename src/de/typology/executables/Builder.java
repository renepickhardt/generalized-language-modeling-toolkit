package de.typology.executables;

import de.typology.splitter.DataSetSplitter;
import de.typology.splitter.IndexBuilder;
import de.typology.splitter.NGramSplitter;
import de.typology.splitter.NGramSplitterWithCount;
import de.typology.splitter.TypoSplitter;
import de.typology.splitter.TypoSplitterWithCount;
import de.typology.stats.StatsBuilder;
import de.typology.utils.Config;

public class Builder {

	public void build(String outputPath) {

		String indexFileName = "index.txt";
		String statsFileName = "stats.txt";
		String trainingFileName = "training.txt";
		String learningFileName = "learning.txt";
		String testingFileName = "testing.txt";

		if (Config.get().splitData) {
			DataSetSplitter dss = new DataSetSplitter();
			dss.split(outputPath + "normalized.txt", outputPath
					+ trainingFileName, outputPath + learningFileName,
					outputPath + testingFileName);
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
