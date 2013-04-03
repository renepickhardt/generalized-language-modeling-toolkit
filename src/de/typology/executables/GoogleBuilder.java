package de.typology.executables;

import java.io.File;
import java.io.IOException;

import de.typology.parser.GoogleMergerMain;
import de.typology.parser.GoogleParserMain;
import de.typology.utils.Config;

public class GoogleBuilder extends Builder {

	/**
	 * executes the following steps:
	 * <p>
	 * 1) parse and normalize google ngram data
	 * <p>
	 * 
	 * @author Rene Pickhardt, Martin Koerner
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		GoogleBuilder gb = new GoogleBuilder();

		File dir = new File(Config.get().googleInputDirectory);
		String outputDirectory = Config.get().outputDirectory + "google/";
		new File(outputDirectory).mkdirs();
		for (File f : dir.listFiles()) {
			String dataSet = f.getName();
			String outputPath = outputDirectory + dataSet + "/";
			File outputFile = new File(outputPath);
			outputFile.mkdirs();
			if (Config.get().parseData) {
				try {
					GoogleMergerMain.run(f.getAbsolutePath(), outputPath);
					GoogleParserMain.run(outputPath, outputPath);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			gb.buildFromNGrams(outputPath);
		}
	}
}
