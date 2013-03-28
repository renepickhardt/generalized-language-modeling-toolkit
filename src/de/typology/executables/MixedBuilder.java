package de.typology.executables;

import java.io.File;
import java.io.IOException;

import de.typology.parser.DGTTMMain;
import de.typology.parser.EnronMain;
import de.typology.utils.Config;

public class MixedBuilder extends Builder {

	/**
	 * executes the following steps:
	 * <p>
	 * 1) parse and normalize enron
	 * <p>
	 * 2) split into training.txt, testing.txt, and learning.txt
	 * <p>
	 * 3) build index.txt
	 * <p>
	 * 4) build ngrams
	 * <p>
	 * 5) build typoedges
	 * 
	 * @author Martin Koerner
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		MixedBuilder mb = new MixedBuilder();
		String outputDirectory;
		String parsedFileName;
		String normalizedFileName;

		// build dgttm
		outputDirectory = Config.get().outputDirectory + "dgttm/";
		parsedFileName = "parsed.txt";
		normalizedFileName = "normalized.txt";
		String[] languages = Config.get().dgttmLanguages.split(",");
		new File(outputDirectory).mkdirs();

		for (String language : languages) {
			String outputPath = outputDirectory + language + "/";
			new File(outputPath).mkdirs();

			if (Config.get().parseData) {
				try {
					DGTTMMain.run(Config.get().dgttmInputDirectory, outputPath
							+ parsedFileName, outputPath + normalizedFileName,
							language);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			mb.build(outputPath);
		}

		// build enron
		outputDirectory = Config.get().outputDirectory + "enron/";
		parsedFileName = "parsed.txt";
		normalizedFileName = "normalized.txt";
		new File(outputDirectory).mkdirs();

		if (Config.get().parseData) {
			try {
				EnronMain.run(Config.get().enronInputDirectory, outputDirectory
						+ parsedFileName, outputDirectory + normalizedFileName);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		mb.build(outputDirectory);
	}
}
