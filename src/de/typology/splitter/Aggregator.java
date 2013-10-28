package de.typology.splitter;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * A class for aggregating sequences by counting their occurrences. Expects an
 * inputStream with a size that is 30% of the allocated main memory.
 * 
 * @author Martin Koerner
 * 
 */
public abstract class Aggregator implements Runnable {
	public Aggregator(InputStream inputStream, OutputStream outputStream,
			char delimiter, int startSortAtColumn) {
		// TODO: add actual constructor

	}

}
