package de.typology.indexes;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * A class that is based on the text file produced by WordIndexer.
 * 
 * @author Martin Koerner
 * 
 */
public class WordIndex {
	protected String[] index;

	public WordIndex(File indexFile) {
		// count total number of lines in the index file
		int lineCount = 0;
		try {
			BufferedReader br = new BufferedReader(new FileReader(indexFile));
			while (br.readLine() != null) {
				lineCount++;
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		this.index = new String[lineCount];
		int currentLineCount = 0;

		// read the index file
		try {
			BufferedReader br = new BufferedReader(new FileReader(indexFile));
			String line;
			String[] lineSplit;
			while ((line = br.readLine()) != null) {
				lineSplit = line.split("\t");
				this.index[currentLineCount] = lineSplit[0];
				currentLineCount++;
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * returns the file in which word should be stored based on this.index
	 * 
	 * @param word
	 * @return
	 */
	public int rank(String word) {
		int lo = 0;
		int hi = this.index.length - 1;
		while (lo <= hi) {
			int mid = lo + (hi - lo) / 2;
			if (word.compareTo(this.index[mid]) < 0) {
				hi = mid - 1;
			} else if (word.compareTo(this.index[mid]) > 0) {
				lo = mid + 1;
			} else {
				return mid;
			}
		}
		// the following return statement is not the standard return result for
		// binary search
		return (lo + hi) / 2;
	}

}
