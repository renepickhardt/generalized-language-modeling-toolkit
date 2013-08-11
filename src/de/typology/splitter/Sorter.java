package de.typology.splitter;

import java.io.File;

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
			String sortCommand = "LANG=C sort --buffer-size=3G --output="
					+ outputPath + " " + inputPath;

			// execute command
			SystemHelper.runUnixCommand(sortCommand);

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
			int columnNumber = Integer.bitCount(Integer.parseInt(
					inputFile.getName().replaceAll("_", "0").split("\\.")[1]
							.split("-")[0], 2));
			String sortCommand = "LANG=C sort --buffer-size=3G ";

			for (int column = 1; column <= columnNumber; column++) {
				sortCommand += "--key=" + column + "," + column + " ";
			}

			sortCommand += "--output=" + outputPath + " " + inputPath;
			// execute command
			SystemHelper.runUnixCommand(sortCommand);

			inputFile.delete();
		}
		// note: when sorting an empty file, new file contains "null1"
	}

	public void sortRevertCountDirectory(String inputPath,
			String inputExtension, String outputExtension) {
		File[] files = new File(inputPath).listFiles();
		for (File file : files) {
			this.sortRevertCountFile(file.getAbsolutePath(), inputExtension,
					outputExtension);
		}
	}

	public void sortRevertCountFile(String inputPath, String inputExtension,
			String outputExtension) {
		if (inputPath.endsWith(inputExtension)) {
			// set output file name
			File inputFile = new File(inputPath);
			String outputPath = inputPath.replace(inputExtension,
					outputExtension);

			// build sort command
			int columnNumber;
			columnNumber = Integer.bitCount(Integer.parseInt(
					inputFile.getName().replaceAll("_", "0").split("\\.")[1]
							.split("-")[0], 2));
			String sortCommand = "LANG=C sort --buffer-size=3G ";

			// don't sort for count
			for (int column = columnNumber; column > 0; column--) {
				sortCommand += "--key=" + column + "," + column + " ";
			}

			sortCommand += "--output=" + outputPath + " " + inputPath;
			// execute command
			SystemHelper.runUnixCommand(sortCommand);

			inputFile.delete();
		}
		// note: when sorting an empty file, new file contains "null1"
	}
}
