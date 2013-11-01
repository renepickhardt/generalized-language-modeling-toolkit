package de.typology.splitter;

import java.io.File;
import java.io.InputStream;

/**
 * A class for splitting a text file (via inputStream) into sequences that are
 * stored in different files based on the indexFile in outputDirectory.
 * 
 * @author Martin Koerner
 * 
 */
public abstract class Sequencer implements Runnable {
	protected InputStream inputStream;
	protected File outputDirectory;
	protected String[] index;
	protected boolean[] pattern;

	public Sequencer(InputStream inputStream, File outputDirectory,
			String[] index, boolean[] pattern) {
		this.inputStream = inputStream;
		this.outputDirectory = outputDirectory;
		this.index = index;
		this.pattern = pattern;
	}
}
