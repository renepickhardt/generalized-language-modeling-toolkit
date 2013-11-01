package de.typology.splitter;

import java.io.BufferedWriter;
import java.util.HashMap;

import de.typology.utils.PatternTransformer;

public class SequencerTask extends Sequencer {

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
