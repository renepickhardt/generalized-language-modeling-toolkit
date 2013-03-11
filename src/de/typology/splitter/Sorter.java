package de.typology.splitter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

import de.typology.utils.IOHelper;

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
			String sortCommand = "sort --buffer-size=3G --output=" + outputPath
					+ " " + inputPath;

			// execute command
			this.executeCommand(sortCommand);

			inputFile.delete();
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
			String sortCommand = "sort --buffer-size=3G ";

			// don't sort for last word (with "< columnNumber - 1")
			for (int column = 1; column < columnNumber - 1; column++) {
				sortCommand += "--key=" + column + "," + column + " ";
			}

			// instead sort for count (nr --> numerics, reverse)
			sortCommand += "--key=" + columnNumber + "," + columnNumber + "nr ";
			sortCommand += "--output=" + outputPath + " " + inputPath;

			// don't sort 1grams (this would sort by count only since the only
			// word is ignored)
			if (columnNumber < 3) {
				sortCommand = "cp " + inputPath + " " + outputPath;
			}

			// execute command
			this.executeCommand(sortCommand);

			inputFile.delete();
		}
		// note: when sorting an empty file, new file contains "null1"
	}

	private int getColumnNumber(String inputPath) {
		BufferedReader br = IOHelper.openReadFile(inputPath);
		int columnNumber = 0;
		try {
			columnNumber = br.readLine().split("\t").length;
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return columnNumber;
	}

	private void executeCommand(String command) {
		Runtime rt = Runtime.getRuntime();
		try {
			rt.exec(new String[] { "bash", "-c", command }).waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
