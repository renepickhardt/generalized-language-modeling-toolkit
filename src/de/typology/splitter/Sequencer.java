package de.typology.splitter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;

import de.typology.indexes.WordIndex;
import de.typology.utils.PatternTransformer;

/**
 * A class for splitting a text file (via inputStream) into sequences that are
 * stored in different files based on the indexFile in outputDirectory.
 * 
 * @author Martin Koerner
 * 
 */
public class Sequencer implements Runnable {
	protected InputStream inputStream;
	protected File outputDirectory;
	protected WordIndex wordIndex;
	protected boolean[] pattern;

	public Sequencer(InputStream inputStream, File outputDirectory,
			WordIndex wordIndex, boolean[] pattern) {
		this.inputStream = inputStream;
		this.outputDirectory = outputDirectory;
		this.wordIndex = wordIndex;
		this.pattern = pattern;
	}

	@Override
	public void run() {
		HashMap<Integer, BufferedWriter> writers = this.openWriters();

	}

	private HashMap<Integer, BufferedWriter> openWriters() {
		HashMap<Integer, BufferedWriter> writers = new HashMap<Integer, BufferedWriter>();
		String stringPattern = PatternTransformer
				.getStringPattern(this.pattern);

		return writers;

	}
}
