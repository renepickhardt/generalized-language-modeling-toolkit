package de.typology.smootherOld;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

import de.typology.utils.IOHelper;

public class SlidingWindowReader {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	public SlidingWindowReader(String filePath, int memoryLimitForReadingFiles) {
		this.reader = IOHelper.openReadFile(filePath,
				memoryLimitForReadingFiles);
		File file = new File(filePath);
		String ending = file.getName().split("\\.")[1].split("-")[0];

		this.sequenceLength = Integer.bitCount(Integer.parseInt(
				ending.replace("_", "0"), 2));
		try {
			// read first line
			this.currentLine = this.reader.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private int sequenceLength;
	private String currentLine;
	private BufferedReader reader;

	public double getCount(String words) {
		while (!this.currentLine.startsWith(words)) {
			try {
				this.currentLine = this.reader.readLine();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		String[] currentLineSplit = this.currentLine.split("\t");
		return Double.parseDouble(currentLineSplit[this.sequenceLength]);
	}

	public double getCount(String words, int columnStartWithZero) {
		while (!this.currentLine.startsWith(words)) {
			try {
				this.currentLine = this.reader.readLine();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		String[] currentLineSplit = this.currentLine.split("\t");
		return Double.parseDouble(currentLineSplit[this.sequenceLength
				+ columnStartWithZero]);
	}

	public void close() {
		try {
			this.reader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
