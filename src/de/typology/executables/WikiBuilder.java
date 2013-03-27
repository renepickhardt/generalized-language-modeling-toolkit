package de.typology.executables;

import java.io.File;
import java.io.IOException;

import de.typology.parser.WikipediaMain;
import de.typology.splitter.DataSetSplitter;
import de.typology.splitter.IndexBuilder;
import de.typology.splitter.NGramSplitter;
import de.typology.splitter.TypoSplitter;
import de.typology.utils.Config;

public class WikiBuilder {

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
		String trainingOutputPath;
		String learningOutputPath;
		String testingOutputPath;
		// parse and normalize wikipedia data:

		File dir = new File(Config.get().wikiInputDirectory);
		String outputDirectory = Config.get().outputDirectory + "wiki/";
		new File(outputDirectory).mkdirs();
		for (File f : dir.listFiles()) {

			String wikiTyp = f.getName().split("-")[0];
			String outputPath = outputDirectory + wikiTyp + "/";
			new File(outputPath).mkdirs();
			parsedOutputPath = outputPath + "parsed.txt";
			normalizedOutputPath = outputPath + "normalized.txt";
			trainingOutputPath = outputPath + "training.txt";
			learningOutputPath = outputPath + "learning.txt";
			testingOutputPath = outputPath + "testing.txt";

			if (Config.get().parseData) {
				WikipediaMain.run(f.getAbsolutePath(), parsedOutputPath,
						normalizedOutputPath);
			}

			if (Config.get().splitData) {
				DataSetSplitter dss = new DataSetSplitter();
				dss.split(outputPath + "normalized.txt", trainingOutputPath,
						learningOutputPath, testingOutputPath);
			}

			if (Config.get().buildNGrams || Config.get().buildTypoEdges) {
				IndexBuilder ib = new IndexBuilder();
				ib.buildIndex(trainingOutputPath, outputPath + "index.txt",
						outputPath + "stats.txt");
			}

			if (Config.get().buildNGrams) {
				NGramSplitter ngs = new NGramSplitter(outputPath, "index.txt",
						"stats.txt", "training.txt");
				ngs.split(5);
			}
			if (Config.get().buildTypoEdges) {
				TypoSplitter ts = new TypoSplitter(outputPath, "index.txt",
						"stats.txt", "training.txt");
				ts.split(5);
			}

		}
	}
}
