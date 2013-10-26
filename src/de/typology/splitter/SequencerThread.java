package de.typology.splitter;

import java.io.File;
import java.io.InputStream;

public class SequencerThread extends Sequencer {

	public SequencerThread(InputStream input, File index, Character delimiter,
			boolean[] pattern, String outputDirectoryName) {
		super(input, index, delimiter, pattern, outputDirectoryName);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub

	}
}
