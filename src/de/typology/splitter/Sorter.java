package de.typology.splitter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

import de.typology.utils.Config;
import de.typology.utils.IOHelper;
import de.typology.utils.SystemHelper;

public class Sorter {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	public void sortSplitDirectory(String inputPath, String inputExtension,
			String outputExtension) {
		File[] files = new File(inputPath).listFiles();
		for (File file : files) {
			this.sortSplitFile(file.getAbsolutePath(), inputExtension,
					outputExtension);
		}
	}

	public void sortSplitFile(String inputPath, String inputExtension,
			String outputExtension) {
		if (inputPath.endsWith(inputExtension)) {
			// set output file name
			File inputFile = new File(inputPath);
			String outputPath = inputPath.replace(inputExtension,
					outputExtension);

			// build sort command
			String sortCommand = "sort --buffer-size=1G --output=" + outputPath
					+ " " + inputPath;

			// execute command
			SystemHelper.runUnixCommand(sortCommand);

			if (Config.get().deleteTemporaryFiles) {
				inputFile.delete();
			}
		}
		// note: when sorting an empty file, new file contains "null1"
	}

	public void sortCountDirectory(String inputPath, String inputExtension,
			String outputExtension) {
		File[] files = new File(inputPath).listFiles();
		for (File file : files) {
			this.sortCountFile(file.getAbsolutePath(), inputExtension,
					outputExtension);
		}
	}

	public void sortCountFile(String inputPath, String inputExtension,
			String outputExtension) {
		if (inputPath.endsWith(inputExtension)) {
			// set output file name
			File inputFile = new File(inputPath);
			String outputPath = inputPath.replace(inputExtension,
					outputExtension);

			// build sort command
			int columnNumber = this.getColumnNumber(inputPath);
			String sortCommand = "sort --buffer-size=1G ";

			// 0edges are only sorted by count
			if (!inputPath.contains(".0")) {
				// don't sort for last word (with "< columnNumber - 1") ...
				for (int column = 1; column < columnNumber - 1; column++) {
					sortCommand += "--key=" + column + "," + column + " ";
				}
			}
			// ... instead sort for count (nr --> numerics, reverse)
			sortCommand += "--key=" + columnNumber + "," + columnNumber + "nr ";
			sortCommand += "--output=" + outputPath + " " + inputPath;

			// execute command
			SystemHelper.runUnixCommand(sortCommand);

			if (Config.get().deleteTemporaryFiles) {
				inputFile.delete();
			}
		}
		// note: when sorting an empty file, new file contains "null1"
	}

	private int getColumnNumber(String inputPath) {
		BufferedReader br = IOHelper.openReadFile(inputPath);
		int columnNumber = 0;
		try {
			String line = br.readLine();
			if (line != null) {
				columnNumber = line.split("\t").length;
			} else {
				columnNumber = 0;
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return columnNumber;
	}
}
