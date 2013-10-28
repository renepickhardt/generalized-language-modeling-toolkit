package de.typology.splitter;

import java.io.File;
import java.io.InputStream;

/**
 * A class for building a text file containing a index representation for a
 * given text file based on the alphabetical distribution of its words.
 * 
 * @author Martin Koerner
 * 
 */
public abstract class Indexer implements Runnable {
	public Indexer(InputStream inputStream, File indexOutputFile,
			int MaxCountDivider) {
		// TODO: add actual constructor
	}

}
