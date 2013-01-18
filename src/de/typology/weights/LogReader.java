package de.typology.weights;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class LogReader {

	/**
	 * buffer for the next item
	 */
	private double[][] nextItem = null;
	
	/**
	 * Buffer to read the input from
	 */
	private BufferedReader in = null;
	
	/**
	 * Cache for the first line of each entry, as it is read during parsing the previous entry (look ahead to notice end of entry), or as it is read as the first line to identify the model count
	 */
	private String cachedLine = null;

	/**
	 * Number of models the log file contains
	 */
	private int modelCount = 0;
	
	/**
	 * Creates the LogReader to read from a given input file 
	 * @param fin
	 */
	public LogReader(File fin) throws IOException {
		this.in = new BufferedReader(new FileReader(fin));
		// read ahead first line
		this.cachedLine = in.readLine();
		// break it into its entries
		String[] fragments = this.cachedLine.trim().split("\\s");
		// There is one entry more (prefix length entry) than models
		this.modelCount = fragments.length -1;
		// prefetch first entry
		this.readNext();
	}
	
	/**
	 * Returns the number of models for which this LogReader will provide results
	 * @return number of models from which it has results
	 */
	public int modelNumber() {
		return this.modelCount;
	}
	
	
	/**
	 * Return the probabilities, first index is prefix length, second index is model length (n-grams)
	 * Returne null if no further intput.
	 * 
	 * @return
	 */
	public  double[][] getNextProbabilities() {
		double[][] result = nextItem;
		this.nextItem = null;
		this.readNext();
		return result;
	}
	
	/**
	 * Internal method to manage the reading of the next entry
	 */
	private void readNext() {
		if (this.in != null) {
			// temporarily cache for the lines of the curent entry
			ArrayList<String> lines = new ArrayList<String>();
			// add its first line, which is in the cache
			lines.add(this.cachedLine);
			// reset the cache to null (we'll use this to close the in buffer at the end of the file)
			this.cachedLine = null;
			String line = null;
			try {
				while ( (line = in.readLine()) != null) {
					if (line.startsWith("0\t")) {
						// next entry -- so cache it and stop reading
						this.cachedLine = line;
						break;
					} else {
						// add to the line cache
						lines.add(line);
					}
				}
				if (this.cachedLine == null) {
					this.in.close();
				}
				// create the object that will be served as next entry
				this.nextItem = new double[lines.size()][this.modelCount];
				for (int i = 0; i < this.nextItem.length; i++) {
					String[] fragments = lines.get(i).trim().split("\\t");
					if (fragments.length -1 != this.modelCount) {
						System.err.println("Wrong number of entries in log file line");
					} else {
						for (int j = 0; j < this.nextItem[i].length; j++) {
							this.nextItem[i][j] = Double.parseDouble(fragments[j+1]);
						}
					}
				}
			} catch (IOException ioe) {
				// Buffer probabily cloed during previous round -- ignore
			}
		}
	}
	
}
