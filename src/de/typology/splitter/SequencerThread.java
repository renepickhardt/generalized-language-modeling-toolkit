package de.typology.splitter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.util.HashMap;

public class SequencerThread extends Sequencer {

	public SequencerThread(InputStream inputStream, File outputDirectory,
			File indexFile, boolean[] pattern) {
		super(inputStream, outputDirectory, indexFile, pattern);
	}

	@Override
	public void run() {

		HashMap<Integer, BufferedWriter> writers = this.openWriters();

	}

	private HashMap<Integer, BufferedWriter> openWriters() {
		HashMap<Integer, BufferedWriter> writers = new HashMap<Integer, BufferedWriter>();
		try {
			BufferedReader indexReader = new BufferedReader(new FileReader(
					this.indexFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		return writers;

	}
}
