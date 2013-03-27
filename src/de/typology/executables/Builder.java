package de.typology.executables;

import java.io.IOException;

import de.typology.parser.WikipediaMain;
import de.typology.splitter.DataSetSplitter;
import de.typology.splitter.IndexBuilder;
import de.typology.splitter.NGramSplitter;
import de.typology.splitter.TypoSplitter;
import de.typology.utils.Config;

public class Builder {

	public void build(String inputPath, String outputPath) {
		String parsedFileName = "parsed.txt";
		String normalizedFileName = "normalized.txt";
		String indexFileName = "index.txt";
		String statsFileName = "stats.txt";
		String trainingFileName = "training.txt";
		String learningFileName = "learning.txt";
		String testingFileName = "testing.txt";

		if (Config.get().parseData) {
			try {
				WikipediaMain.run(inputPath, outputPath + parsedFileName,
						outputPath + normalizedFileName);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		if (Config.get().splitData) {
			DataSetSplitter dss = new DataSetSplitter();
			dss.split(outputPath + "normalized.txt", outputPath
					+ trainingFileName, outputPath + learningFileName,
					outputPath + testingFileName);
		}

		if (Config.get().buildNGrams || Config.get().buildTypoEdges) {
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
}
