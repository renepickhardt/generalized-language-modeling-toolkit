package de.typology.googleNGrams;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import de.typology.utils.IOHelper;

public class NGramParserMain {
	public static void main(String[] args) throws IOException {

	}

	public static void run(String googleInputPath,
			String parsedOutputPath, String normalizedOutputPath)
			throws IOException {

		ArrayList<File> files = IOHelper
				.getDirectory(new File(googleInputPath));
		for (File file : files) {
			long startTime = System.currentTimeMillis();
			long endTime = 0;
			long sek = 0;
			NGramRecognizer recognizer = new NGramRecognizer(
					file.getAbsolutePath());
			NGramParser parser = new NGramParser(recognizer,
					parsedOutputPath);
			System.out.println("start parsing");
			parser.parse();
			System.out.println("parsing done");
			NGramNormalizer ngn = new NGramNormalizer(parsedOutputPath,
					normalizedOutputPath);
			System.out.println("start cleanup");
			ngn.normalize();
			System.out.println("cleanup done");
			System.out.println("generate indicator file");
			endTime = System.currentTimeMillis();
			sek = (endTime - startTime) / 1000;
			IOHelper.strongLog("done normalizing: " + parsedOutputPath
					+ ", time: " + sek + " seconds");
		}
		System.out.println("done");
	}
}
