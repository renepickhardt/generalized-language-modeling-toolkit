package de.typology.splitter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public class GLMCounter {

	protected File directory;
	protected File inputDirectory;
	protected File outputDirectory;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		GLMCounter glmc = new GLMCounter(Config.get().outputDirectory
				+ Config.get().inputDataSet, "absolute", "counts-absolute");
		glmc.countAbsolute();
		System.out.println(glmc.getAbsoluteCount("1"));

	}

	public GLMCounter(String directory, String inputDirectoryName,
			String outputDirectoryName) {
		this.directory = new File(directory);
		this.inputDirectory = new File(directory + inputDirectoryName);
		System.out.println(this.inputDirectory.getAbsolutePath());
		this.outputDirectory = new File(directory + outputDirectoryName);
		if (this.outputDirectory.exists()) {
			try {
				FileUtils.deleteDirectory(this.outputDirectory);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		this.outputDirectory.mkdir();
	}

	public void countAbsolute() {
		for (File currentInputDirectory : this.inputDirectory.listFiles()) {
			long count = 0;
			try {
				for (File inputFile : currentInputDirectory.listFiles()) {
					BufferedReader reader = IOHelper.openReadFile(inputFile
							.getAbsolutePath());
					String line;
					String[] lineSplit;
					while ((line = reader.readLine()) != null) {
						lineSplit = line.split("\t");
						count += Integer
								.parseInt(lineSplit[lineSplit.length - 1]);
					}
					reader.close();
				}
				BufferedWriter writer = IOHelper
						.openWriteFile(this.outputDirectory + "/"
								+ currentInputDirectory.getName() + ".cnt");
				writer.write(count + "\n");
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public long getAbsoluteCount(String type) {
		File resultFile = new File(this.outputDirectory + "/" + type + ".cnt");
		if (resultFile.exists()) {
			BufferedReader reader = IOHelper.openReadFile(resultFile
					.getAbsolutePath());
			try {
				return Long.parseLong(reader.readLine());
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return 0;

	}
}
