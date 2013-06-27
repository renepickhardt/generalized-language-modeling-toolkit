package de.typology.parser;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public class WikipediaMain {

	/**
	 * @author Martin Koerner
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		File dir = new File(Config.get().wikiInputDirectory);
		String outputDirectory = Config.get().outputDirectory + "wiki/";
		new File(outputDirectory).mkdirs();
		for (File file : dir.listFiles()) {
			String dataSet = file.getName().split("-")[0];
			dataSet = dataSet.replace("wiki", "");
			new File(outputDirectory + dataSet).mkdirs();
			run(file.getAbsolutePath(), outputDirectory + dataSet
					+ "/parsed.txt", outputDirectory + dataSet
					+ "/normalized.txt");
		}
	}

	public static void run(String wikiInputPath, String parsedOutputPath,
			String normalizedOutputPath) throws IOException {

		long startTime = System.currentTimeMillis();
		// WikipediaTokenizer tokenizer = new WikipediaTokenizer(wikiInputPath);
		// WikipediaRecognizer recognizer = new WikipediaRecognizer(tokenizer);
		// WikipediaParser parser = new WikipediaParser(recognizer,
		// parsedOutputPath);
		// IOHelper.log("start parsing: " + wikiInputPath);
		// parser.parse();
		// IOHelper.log("parsing done");
		IOHelper.log("start cleanup");
		WikipediaNormalizer wn = new WikipediaNormalizer(parsedOutputPath,
				normalizedOutputPath, WikipediaMain.getLocale(wikiInputPath));
		wn.normalize();
		IOHelper.log("cleanup done");
		IOHelper.log("generate indicator file");
		long endTime = System.currentTimeMillis();
		long time = (endTime - startTime) / 1000;
		File done = new File(normalizedOutputPath + "IsDone." + time + "s");
		done.createNewFile();
		IOHelper.log("done");

	}

	public static Locale getLocale(String inputPath) {
		System.out.println(inputPath);
		String fileName = new File(inputPath).getName();
		String type = fileName.split("-")[0];
		if (type.startsWith("bar")) {
			return Locale.GERMAN;
		}
		if (type.startsWith("de")) {
			return Locale.GERMAN;
		}
		if (type.startsWith("en")) {
			return Locale.ENGLISH;
		}
		if (type.startsWith("it")) {
			return Locale.ITALIAN;
		}
		if (type.startsWith("fr")) {
			return Locale.FRENCH;
		}
		throw new IllegalStateException("Could not match language");
	}
}
