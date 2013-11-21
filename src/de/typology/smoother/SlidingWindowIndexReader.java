package de.typology.smoother;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import de.typology.indexes.WordIndex;
import de.typology.utils.SlidingWindowReader;

/**
 * This class is able to switch between files based on a WordIndex. It returns
 * the wanted line by using SlidingWindowReader.
 * 
 * @author Martin Koerner
 * 
 */
public class SlidingWindowIndexReader {
	private File inputDirectory;
	private int bufferSize;
	private WordIndex wordIndex;

	private SlidingWindowReader slidingWindowReader;
	private int currentReaderRank;

	public SlidingWindowIndexReader(File inputDirectory, WordIndex wordIndex) {
		this.inputDirectory = inputDirectory;
		this.bufferSize = 0;
		this.wordIndex = wordIndex;
	}

	public SlidingWindowIndexReader(File inputDirectory, int bufferSize,
			WordIndex wordIndex) {
		this.inputDirectory = inputDirectory;
		this.bufferSize = bufferSize;
		this.wordIndex = wordIndex;
	}

	public String getLine(String inputString) throws IOException {
		int currentRank = this.wordIndex.rank(inputString.split("\\s")[0]);

		// switch to new file if inputString has a different rank than the
		// current reader
		if (currentRank != this.currentReaderRank) {

			// close existing reader
			if (this.slidingWindowReader != null) {
				this.slidingWindowReader.close();
			}

			// open new reader
			if (this.bufferSize == 0) {
				this.slidingWindowReader = new SlidingWindowReader(
						new FileReader(this.inputDirectory.getAbsolutePath()
								+ "/" + currentRank));
			} else {
				this.slidingWindowReader = new SlidingWindowReader(
						new FileReader(this.inputDirectory.getAbsolutePath()
								+ "/" + currentRank), this.bufferSize);
			}

			this.currentReaderRank = currentRank;
		}

		return this.slidingWindowReader.getLine(inputString);
	}

	public void close() throws IOException {
		this.slidingWindowReader.close();
	}
}
