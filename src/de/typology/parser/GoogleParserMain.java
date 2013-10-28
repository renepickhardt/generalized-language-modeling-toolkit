package de.typology.parser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import de.typology.utils.IOHelper;

public class GoogleParserMain {
	public static void main(String[] args) throws IOException {

	}

	public static void run(String googleInputPath, String outputPath)
			throws IOException {

		IOHelper.log("getting file list");
		ArrayList<File> files = IOHelper
				.getDirectory(new File(googleInputPath));

		for (File file : files) {
			long startTime = System.currentTimeMillis();
			long endTime = 0;
			long sek = 0;
			GoogleRecognizer recognizer = new GoogleRecognizer(
					file.getAbsolutePath());
			System.out.println("File:" + file.getName());
			String fileName = file.getName().split("-")[0];
			int nGramCount = Integer.valueOf(fileName.substring(0, 1));

			String parsedOutputName = outputPath + fileName + "-parsed.txt";
			String normalizedOutputName = outputPath + fileName
					+ "-normalized.txt";
			System.out.println(parsedOutputName);
			GoogleParser parser = new GoogleParser(recognizer, parsedOutputName);
			IOHelper.log("start parsing");
			parser.parse();
			IOHelper.log("parsing done");
			GoogleNormalizer ngn = new GoogleNormalizer(parsedOutputName,
					normalizedOutputName, nGramCount);
			IOHelper.log("start cleanup");
			ngn.normalize();
			IOHelper.log("cleanup done");
			// if (Config.get().generateNGramDistribution) {
			// NGramDistribution ngd = new NGramDistribution();
			// ngd.countDistribution(outputPath+"normalized/", ".txt");
			// endTime = System.currentTimeMillis();
			// sek = (endTime - startTime) / 1000;
			// IOHelper.strongLog(sek
			// + " seconds to: finnish creating distribution for ngrams");
			// }
			IOHelper.log("generate indicator file");
			endTime = System.currentTimeMillis();
			sek = (endTime - startTime) / 1000;
			IOHelper.strongLog("done normalizing: " + file.getAbsolutePath()
					+ ", time: " + sek + " seconds");
		}
	}
}
