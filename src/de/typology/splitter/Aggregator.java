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

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String dataSet = "testwiki";
		TypoSplitter ngs = new TypoSplitter(Config.get().outputDirectory
				+ dataSet + "/index.txt", Config.get().outputDirectory
				+ dataSet + "/normalized.txt");
		ngs.split(5);
		Aggregator a = new Aggregator();
		a.aggregateFile("filename", "es_split", "es_aggregate");

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
				// initializing current and previous
				this.currentLine = this.reader.readLine();
				if (this.currentLine.equals("null1")) {
					IOHelper.log("skipping empty file: " + inputFile.getName());
					this.reader.close();
					this.writer.close();
					// inputFile.delete();
					return;
				}
				this.previousLine = this.currentLine;
				this.lineCount = 1;
				while ((this.currentLine = this.reader.readLine()) != null) {
					if (this.currentLine.equals(this.previousLine)) {
						this.lineCount++;
					} else {
						this.writer.write(this.previousLine + this.lineCount
								+ "\n");
						this.lineCount = 1;
					}
					this.previousLine = this.currentLine;
				}
				// write last line
				this.writer.write(this.previousLine + this.lineCount + "\n");

				this.reader.close();
				this.writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			// inputFile.delete();
		}
	}
}
