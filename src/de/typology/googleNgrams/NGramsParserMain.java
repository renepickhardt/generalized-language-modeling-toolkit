package de.typology.googleNgrams;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public class NGramsParserMain {
	public static void main(String[] args) throws IOException {
		long startTime = System.currentTimeMillis();
		long endTime = 0;
		long sek = 0;
		ArrayList<File> files = IOHelper.getDirectory(new File(
				Config.get().googleNgramsPath));
		for (File file : files) {
			NGramRecognizer recognizer = new NGramRecognizer(
					file.getAbsolutePath());
			NGramParser parser = new NGramParser(recognizer);
			System.out.println("start parsing");
			parser.parse();
			System.out.println("parsing done");
			String NGramNormalizerOutput = file.getAbsolutePath().substring(0,
					file.getAbsolutePath().length() - 4)
					+ "normalized.txt";
			NGramNormalizer ngn = new NGramNormalizer(
					Config.get().parsedGoogleNGramsOutputPath,
					NGramNormalizerOutput);
			System.out.println("start cleanup");
			ngn.normalize();
			System.out.println("cleanup done");
			System.out.println("generate indicator file");
			endTime = System.currentTimeMillis();
			sek = (endTime - startTime) / 1000;
			File done = new File(NGramNormalizerOutput + "IsDone." + sek + "s");
			done.createNewFile();
		}
		System.out.println("done");
	}
}
