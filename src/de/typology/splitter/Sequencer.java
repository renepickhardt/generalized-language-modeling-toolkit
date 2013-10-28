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
	protected File outputDreictory;
	protected File indexFile;
	protected boolean[] pattern;

	public Sequencer(InputStream inputStream, File outputDirectory,
			File indexFile, boolean[] pattern) {
		this.inputStream = inputStream;
		this.outputDreictory = outputDirectory;
		this.indexFile = indexFile;
		this.pattern = pattern;
	}
}
