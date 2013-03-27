package de.typology.splitter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public class Aggregator {
	private BufferedReader reader;
	private BufferedWriter writer;

	private String previousLine;
	private String currentLine;
	private int lineCount;
	private int currentLineCount;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// String dataSet = "testwiki";
		// TypoSplitter ngs = new TypoSplitter(Config.get().outputDirectory
		// + dataSet + "/index.txt", Config.get().outputDirectory
		// + dataSet + "/normalized.txt");
		// ngs.split(5);
		// Aggregator a = new Aggregator();
		// a.aggregateFile("filename", "es_split", "es_aggregate");

	}

	public Aggregator() {
	}

	public void aggregateDirectory(String inputPath, String inputExtension,
			String outputExtension) {
		File[] files = new File(inputPath).listFiles();
		for (File file : files) {
			this.aggregateFile(file.getAbsolutePath(), inputExtension,
					outputExtension);
		}
	}

	public void aggregateFile(String inputPath, String inputExtension,
			String outputExtension) {
		if (inputPath.endsWith(inputExtension)) {
			File inputFile = new File(inputPath);
			this.reader = IOHelper.openReadFile(inputPath, 1024 * 1024 * 8);
			this.writer = IOHelper.openWriteFile(inputFile.getAbsolutePath()
					.replace(inputExtension, outputExtension), 1024 * 1024 * 8);
			try {
				// initializing currentLine and currentLineCount
				if (!this.setLineAndCount()) {
					IOHelper.log("skipping empty file: " + inputFile.getName());
					this.reader.close();
					this.writer.close();
					inputFile.delete();
					return;
				}

				// initialize previousLine and lineCount
				this.previousLine = this.currentLine;
				this.lineCount = this.currentLineCount;

				while (this.setLineAndCount()) {
					if (this.currentLine.equals(this.previousLine)) {
						this.lineCount += this.currentLineCount;
					} else {
						this.writer.write(this.previousLine + this.lineCount
								+ "\n");
						this.lineCount = this.currentLineCount;
					}
					this.previousLine = this.currentLine;
				}
				// write last line
				this.writer.write(this.previousLine + this.lineCount + "\n");

				this.reader.close();
				this.writer.close();
				if (Config.get().deleteTemporaryFiles) {
					inputFile.delete();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	private boolean setLineAndCount() {
		String tempLine;
		String[] tempLineSplit;
		this.currentLine = "";
		try {
			if ((tempLine = this.reader.readLine()) != null) {
				tempLineSplit = tempLine.split("\t");
				// tempLineSplit.length-1 to exclude the count
				for (int i = 0; i < tempLineSplit.length - 1; i++) {
					this.currentLine += tempLineSplit[i] + "\t";
				}
				this.currentLineCount = Integer
						.parseInt(tempLineSplit[tempLineSplit.length - 1]);
				return true;
			} else {
				return false;
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
}
