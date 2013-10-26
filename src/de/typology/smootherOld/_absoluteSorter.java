package de.typology.smootherOld;

import java.io.File;

import de.typology.splitterOld.Sorter;
import de.typology.utils.SystemHelper;

public class _absoluteSorter extends Sorter {

	/**
	 * This class provides a method for sorting a given file by the second,
	 * third, fourth...(, first) word in order to calculate the novel
	 * continuation probability used in Kneser-Ney interpolation.
	 * 
	 * @param args
	 * 
	 * @author Martin Koerner
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	public void sortSecondCloumnDirectory(String inputPath,
			String inputExtension, String outputExtension) {
		File[] files = new File(inputPath).listFiles();
		for (File file : files) {
			this.sortSecondCloumnFile(file.getAbsolutePath(), inputExtension,
					outputExtension);
		}
	}

	public void sortSecondCloumnFile(String inputPath, String inputExtension,
			String outputExtension) {
		if (inputPath.endsWith(inputExtension)) {
			// set output file name
			File inputFile = new File(inputPath);
			String outputPath = inputPath.replace(inputExtension,
					outputExtension);
			// build sort command
			int columnNumber = Integer.bitCount(Integer.parseInt(inputFile
					.getName().split("\\.")[1].split("-")[0], 2));
			// LANG=C to sort utf-8 correctly
			String sortCommand = "LANG=C sort --buffer-size=3G ";

			for (int column = 2; column <= columnNumber; column++) {
				sortCommand += "--key=" + column + "," + column + " ";
			}
			// sort for first line
			sortCommand += "--key=1,1 ";
			sortCommand += "--output=" + outputPath + " " + inputPath;

			// execute command
			SystemHelper.runUnixCommand(sortCommand);

			inputFile.delete();
		}
		// note: when sorting an empty file, new file contains "null1"
	}

}
