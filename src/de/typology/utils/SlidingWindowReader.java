package de.typology.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

/**
 * This reader searches for a string in a sliding window manner. The input file
 * needs to be sorted alphabetically and the searched string has to appear after
 * the line that was read last.
 * 
 * @author Martin Koerner
 * 
 */
public class SlidingWindowReader extends BufferedReader {

	private String currentLine;

	public SlidingWindowReader(Reader in, int sz) {
		super(in, sz);
		// TODO Auto-generated constructor stub
	}

	public SlidingWindowReader(Reader in) {
		super(in);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Returns the line that starts with words. Only works when words is still
	 * to read from the file.
	 * 
	 * @param inputString
	 * @return
	 * @throws IOException
	 */
	public String getLine(String inputString) throws IOException {
		while (!this.currentLine.startsWith(inputString)) {
			this.currentLine = this.readLine();
		}
		return this.currentLine;
	}
}
