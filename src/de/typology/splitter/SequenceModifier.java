package de.typology.splitter;

import java.io.File;
import java.io.OutputStream;

/**
 * A class for modifying the sequences in InputDirectory based on the given
 * Pattern. The modified sequences are returned as outputStream
 * 
 * @author Martin Koerner
 * 
 */
public abstract class SequenceModifier implements Runnable {
	public SequenceModifier(File inputDirectory, OutputStream outputStream,
			Character delimiter, boolean[] pattern, String outputDirectoryName) {
		// TODO: add actual constructor
	}

}
