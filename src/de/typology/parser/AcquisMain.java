package de.typology.parser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public class AcquisMain {
	private static ArrayList<File> fileList;

	/**
	 * @author Martin Koerner
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		File outputDirectory = new File(Config.get().outputDirectory
				+ "acquis/");
		String[] languages = Config.get().acquisLanguages.split(",");
		outputDirectory.mkdirs();

		for (String language : languages) {
			String outputPath = outputDirectory.getAbsolutePath() + "/"
					+ language;
			new File(outputPath).mkdirs();

			try {
				AcquisMain.run(Config.get().acquisInputDirectory, outputPath
						+ "/parsed.txt", outputPath + "/normalized.txt",
						language);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void run(String acquisInputPath, String parsedOutputPath,
			String normalizedOutputPath, String acquisLanguage)
			throws IOException {
		long startTime = System.currentTimeMillis();
		IOHelper.log("getting file list");
		fileList = IOHelper.getDirectory(new File(acquisInputPath));

		AcquisParser parser = new AcquisParser(fileList, parsedOutputPath,
				acquisLanguage);
		IOHelper.log("start parsing: " + acquisInputPath);
		parser.parse();
		IOHelper.log("parsing done");
		IOHelper.log("start cleanup");
		AcquisNormalizer wn = new AcquisNormalizer(parsedOutputPath,
				normalizedOutputPath, AcquisMain.getLocale(acquisLanguage));
		wn.normalize();
		IOHelper.log("cleanup done");
		IOHelper.log("generate indicator file");
		long endTime = System.currentTimeMillis();
		long time = (endTime - startTime) / 1000;
		IOHelper.strongLog("done normalizing: " + acquisInputPath + ", time: "
				+ time + " seconds");
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
