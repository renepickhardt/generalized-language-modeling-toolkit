package de.typology.executablesOld;

import java.io.File;
import java.io.IOException;

import de.typology.utilsOld.Config;

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

		// build acquis
		outputDirectory = Config.get().outputDirectory + "acquis/";
		parsedFileName = "parsed.txt";
		normalizedFileName = "normalized.txt";
		String[] languages = Config.get().acquisLanguages.split(",");
		new File(outputDirectory).mkdirs();

		for (String language : languages) {
			String outputPath = outputDirectory + language + "/";
			new File(outputPath).mkdirs();

			mb.build(outputPath);
		}

		// build enron
		outputDirectory = Config.get().outputDirectory + "enron/en/";
		parsedFileName = "parsed.txt";
		normalizedFileName = "normalized.txt";
		new File(outputDirectory).mkdirs();

		mb.build(outputDirectory);

		// build reuters
		outputDirectory = Config.get().outputDirectory + "reuters/en/";
		parsedFileName = "parsed.txt";
		normalizedFileName = "normalized.txt";
		new File(outputDirectory).mkdirs();

		mb.build(outputDirectory);
	}
}
