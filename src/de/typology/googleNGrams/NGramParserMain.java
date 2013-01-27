package de.typology.googleNGrams;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import de.typology.stats.NGramDistribution;
import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public class NGramParserMain {
	public static void main(String[] args) throws IOException {

	}

	public static void run(String googleInputPath, String outputPath) throws IOException {

		IOHelper.log("getting file list");
		ArrayList<File> files = IOHelper
				.getDirectory(new File(googleInputPath));

		new File(outputPath+"parsed/").mkdirs();
		new File(outputPath+"normalized/").mkdirs();
		for (File file : files) {
			long startTime = System.currentTimeMillis();
			long endTime = 0;
			long sek = 0;
			NGramRecognizer recognizer = new NGramRecognizer(
					file.getAbsolutePath());
			System.out.println("File:"+ file.getName());
			String fileName=file.getName().split("-")[0];
			String parsedOutputName= outputPath+"parsed/"+fileName+"-parsed.txt";
			String normalizedOutputName= outputPath+"normalized/"+fileName+"-normalized.txt";
			System.out.println(parsedOutputName);
			NGramParser parser = new NGramParser(recognizer, parsedOutputName);
			IOHelper.log("start parsing");
			parser.parse();
			IOHelper.log("parsing done");
			NGramNormalizer ngn = new NGramNormalizer(parsedOutputName,
					normalizedOutputName);
			IOHelper.log("start cleanup");
			ngn.normalize();
			IOHelper.log("cleanup done");
			if (Config.get().generateNGramDistribution) {
				NGramDistribution ngd = new NGramDistribution();
				ngd.countDistribution(outputPath+"normalized/", ".txt");
				endTime = System.currentTimeMillis();
				sek = (endTime - startTime) / 1000;
				IOHelper.strongLog(sek
						+ " seconds to: finnish creating distribution for ngrams");
			}
			IOHelper.log("generate indicator file");
			endTime = System.currentTimeMillis();
			sek = (endTime - startTime) / 1000;
			IOHelper.strongLog("done normalizing: " + file.getAbsolutePath()
					+ ", time: " + sek + " seconds");
		}
	}
}
