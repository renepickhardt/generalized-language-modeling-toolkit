package de.typology.executables;

import java.io.IOException;

public class MixedNGramBuilder extends Builder {

	/**
	 * executes the following steps:
	 * <p>
	 * 1) parse and normalize enron
	 * <p>
	 * 2) split into training.txt, testing.txt, and learning.txt
	 * <p>
	 * 3) build index.txt
	 * <p>
	 * 4) build ngrams
	 * <p>
	 * 5) build typoedges
	 * 
	 * @author Martin Koerner
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
	}
}
