package de.typology.stats;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public class StatsBuilder {
	private BufferedReader reader;
	private BufferedWriter statsWriter;
	private long wordCount;
	private String line;
	private String[] lineSplit;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		StatsBuilder sb = new StatsBuilder();
		String outputPath = Config.get().outputDirectory + "google/ger/";
		sb.buildStats(outputPath + "1gram-normalized.txt", outputPath
				+ Config.get().statsName);
	}

	public void buildStats(String inputPath, String outputPath) {
		this.reader = IOHelper.openReadFile(inputPath);
		this.statsWriter = IOHelper.openWriteFile(outputPath);
		this.wordCount = 0;
		try {
			while ((this.line = this.reader.readLine()) != null) {
				this.lineSplit = this.line.split("\t");
				this.wordCount += Long.parseLong(this.lineSplit[1]);
			}
			this.reader.close();
			this.statsWriter.write(inputPath + ":\n");
			this.statsWriter.write("total words: " + this.wordCount + "\n");
			this.statsWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
