package de.typology.smoother;

import java.io.File;
import java.io.IOException;

import de.typology.splitter.Sorter;

public class ContinuationSorter extends Sorter {

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
			int columnNumber = this.getColumnNumber(inputPath);
			// LANG=C to sort utf-8 correctly
			String sortCommand = "LANG=C sort --buffer-size=1G ";

			// don't sort for last column (columnnumber - 1) just yet
			for (int column = 2; column < columnNumber; column++) {
				sortCommand += "--key=" + column + "," + column + " ";
			}
			// sort for count (nr --> numerics, reverse)
			sortCommand += "--key=" + columnNumber + "," + columnNumber + "nr ";
			// sort for first line
			sortCommand += "--key=1,1 ";
			sortCommand += "--output=" + outputPath + " " + inputPath;

			// execute command
			Process p;
			try {
				p = Runtime.getRuntime().exec(
						new String[] { "/bin/sh", "-c", sortCommand });
				p.waitFor();
				p.destroy();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			inputFile.delete();
		}
		// note: when sorting an empty file, new file contains "null1"
	}

}