package de.typology.smoother;

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
	}

	private int sequenceLength;
	private String currentLine;
	private BufferedReader reader;
	private double currentLineCount;

	public double getCount(String words) {
		try {
			if (this.currentLine == null) {
				this.currentLine = this.reader.readLine();
				String[] currentLineSplit = this.currentLine.split("\t");
				this.currentLineCount = Double
						.parseDouble(currentLineSplit[this.sequenceLength]);
			}
			if (this.currentLine.startsWith(words)) {
				return this.currentLineCount;
			} else {
				this.currentLine = this.reader.readLine();
				String[] currentLineSplit = this.currentLine.split("\t");
				this.currentLineCount = Double
						.parseDouble(currentLineSplit[this.sequenceLength]);
				return this.getCount(words);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		throw new IllegalStateException();
	}

	public void printFile() {
		String line;
		try {
			while ((line = this.reader.readLine()) != null) {
				System.out.println(line);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
