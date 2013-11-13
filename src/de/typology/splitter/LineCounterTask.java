package de.typology.splitter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LineCounterTask implements Runnable {
	protected InputStream inputStream;
	protected File outputDirectory;
	protected String patternLabel;
	protected String delimiter;

	static Logger logger = LogManager.getLogger(SmoothingSplitter.class
			.getName());

	public LineCounterTask(InputStream inputStream, File outputDirectory,
			String patternLabel, String delimiter) {
		this.inputStream = inputStream;
		this.outputDirectory = outputDirectory;
		this.patternLabel = patternLabel;
		this.delimiter = delimiter;
	}

	@Override
	public void run() {
		File outputDirectory = new File(this.outputDirectory.getAbsolutePath()
				+ "/" + this.patternLabel);
		if (outputDirectory.exists()) {
			try {
				FileUtils.deleteDirectory(outputDirectory);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		outputDirectory.mkdir();
		logger.info("count lines for: " + outputDirectory.getAbsolutePath());

		BufferedReader inputStreamReader = new BufferedReader(
				new InputStreamReader(this.inputStream));
		long lineCount = 0L;
		try {
			while (inputStreamReader.readLine() != null) {
				lineCount++;
			}
			inputStreamReader.close();

			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(
					outputDirectory.getAbsolutePath() + "/" + "all"));
			bufferedWriter.write(lineCount + "\n");
			bufferedWriter.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
