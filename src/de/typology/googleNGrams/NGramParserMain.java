package de.typology.googleNGrams;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import de.typology.utils.IOHelper;

public class NGramParserMain {
	public static void main(String[] args) throws IOException {

	}

	public static void run(String googleInputPath, String parsedOutputPath,
			String normalizedOutputPath) throws IOException {
		IOHelper.log("getting file list");
		ArrayList<File> files = IOHelper
				.getDirectory(new File(googleInputPath));
		for (File file : files) {
			long startTime = System.currentTimeMillis();
			long endTime = 0;
			long sek = 0;
			NGramRecognizer recognizer = new NGramRecognizer(
					file.getAbsolutePath());
			NGramParser parser = new NGramParser(recognizer, parsedOutputPath);
			IOHelper.log("start parsing");
			parser.parse();
			IOHelper.log("parsing done");
			NGramNormalizer ngn = new NGramNormalizer(parsedOutputPath,
					normalizedOutputPath);
			IOHelper.log("start cleanup");
			ngn.normalize();
			IOHelper.log("cleanup done");
			IOHelper.log("generate indicator file");
			endTime = System.currentTimeMillis();
			sek = (endTime - startTime) / 1000;
			IOHelper.strongLog("done normalizing: " + googleInputPath
					+ ", time: " + sek + " seconds");
		}
	}
}
