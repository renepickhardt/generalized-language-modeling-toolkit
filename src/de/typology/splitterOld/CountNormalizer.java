package de.typology.splitterOld;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import de.typology.utils.IOHelper;

public class CountNormalizer {
	private BufferedReader reader;
	private BufferedWriter writer;

	private String previousHead;
	private String currentLine;
	private String[] currentLineSplit;
	private String currentHead;
	private Long headCount;

	private ArrayList<String> tailList;
	private ArrayList<Long> countList;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	public void normalizeDirectory(String statsPath, String inputPath,
			String inputExtension, String outputExtension) {
		File[] files = new File(inputPath).listFiles();
		for (File file : files) {
			this.normalizeFile(statsPath, file.getAbsolutePath(),
					inputExtension, outputExtension);
		}
	}

	public void normalizeFile(String statsPath, String inputPath,
			String inputExtension, String outputExtension) {
		if (inputPath.endsWith(inputExtension)) {
			File inputFile = new File(inputPath);
			this.reader = IOHelper.openReadFile(inputPath, 1024 * 1024 * 8);
			this.writer = IOHelper.openWriteFile(inputFile.getAbsolutePath()
					.replace(inputExtension, outputExtension), 1024 * 1024 * 8);
			try {
				// read first line for proper initialization
				if ((this.currentLine = this.reader.readLine()) == null) {
					this.reader.close();
					this.writer.close();
					inputFile.delete();
					return;
				}
				this.currentLineSplit = this.currentLine.split("\t");

				// leave out 1grams and 0edges
				if (this.currentLineSplit.length < 3
						|| inputPath.contains(".0")) {

					// get total count from stats file
					BufferedReader statsReader = IOHelper.openReadFile(
							statsPath, 1024 * 1024 * 8);
					Long totalCount = 0L;
					String tempLine;
					while ((tempLine = statsReader.readLine()) != null) {
						if (tempLine.startsWith("total words: ")) {
							totalCount = Long.parseLong(tempLine.replace(
									"total words: ", ""));
						}
					}
					do {
						this.currentLineSplit = this.currentLine.split("\t");
						this.headCount = Long
								.parseLong(this.currentLineSplit[this.currentLineSplit.length - 1]);
						for (int i = 0; i < this.currentLineSplit.length - 1; i++) {
							this.writer.write(this.currentLineSplit[i] + "\t");
						}
						this.writer.write((double) this.headCount / totalCount
								+ "\n");
					} while ((this.currentLine = this.reader.readLine()) != null);
					this.reader.close();
					this.writer.close();
					inputFile.delete();
					return;
				}

				this.buildHead();
				// initializing current and previous
				this.previousHead = this.currentHead;
				this.resetTailsAndCounts();
				this.addTail();

				while ((this.currentLine = this.reader.readLine()) != null) {
					this.currentLineSplit = this.currentLine.split("\t");
					this.buildHead();
					if (this.currentHead.equals(this.previousHead)) {
						this.addTail();
					} else {
						// write lines with normalized values
						this.writeLines();
						this.resetTailsAndCounts();
						this.addTail();
					}
					this.previousHead = this.currentHead;
				}
				// write last line
				this.writeLines();

				this.reader.close();
				this.writer.close();
				inputFile.delete();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void buildHead() {
		this.currentHead = "";
		for (int i = 0; i < this.currentLineSplit.length - 2; i++) {
			this.currentHead += this.currentLineSplit[i] + "\t";
		}
	}

	private void resetTailsAndCounts() {
		this.tailList = new ArrayList<String>();
		this.countList = new ArrayList<Long>();
		this.headCount = 0L;
	}

	private void addTail() {
		this.tailList
				.add(this.currentLineSplit[this.currentLineSplit.length - 2]);
		this.countList
				.add(Long
						.parseLong(this.currentLineSplit[this.currentLineSplit.length - 1]));
		this.headCount += Long
				.parseLong(this.currentLineSplit[this.currentLineSplit.length - 1]);
	}

	private void writeLines() {
		for (String tail : this.tailList) {
			try {
				this.writer.write(this.previousHead + tail + "\t"
						+ (double) this.countList.remove(0) / this.headCount
						+ "\n");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}
}
