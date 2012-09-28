package de.typology.googleNgrams;

import java.io.File;
import java.io.IOException;

import de.typology.utils.Config;

public class NGramsParserMain {
	public static void main(String[] args) throws IOException {
		long startTime = System.nanoTime();
		NGramRecognizer recognizer = new NGramRecognizer(
				Config.get().googleNgramsPath);
		NGramParser parser = new NGramParser(recognizer);
		System.out.println("start parsing");
		parser.parse();
		System.out.println("parsing done");
		NGramNormalizer ngn = new NGramNormalizer(
				Config.get().parsedGoogleNGramsOutputPath,
				Config.get().normalizedGoogleNgramsPath);
		System.out.println("start cleanup");
		ngn.normalize();
		System.out.println("cleanup done");
		System.out.println("generate indicator file");
		long endTime = System.nanoTime();
		long time = Math.round((double) (endTime - startTime) / 1000) / 1000;
		File done = new File(Config.get().normalizedGoogleNgramsPath
				+ "IsDone." + time + "ms");
		done.createNewFile();
		System.out.println("done");
	}
}
