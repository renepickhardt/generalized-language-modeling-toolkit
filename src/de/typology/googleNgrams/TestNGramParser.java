package de.typology.googleNgrams;

import java.io.IOException;

import de.typology.utils.Config;

public class TestNGramParser {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {

		NGramNormalizer ngn = new NGramNormalizer(
				Config.get().parsedGoogleNGramsOutputPath,
				Config.get().normalizedGoogleNgramsPath);
		ngn.normalize();

	}
}
