package de.typology.splitter;

import java.io.File;
import java.io.IOException;

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
			File inputFile = new File(inputPath);
			String outputPath = inputPath.replace(inputExtension,
					outputExtension);
			System.out.println(outputPath);
			String sortCommand = "sort -S3G " + inputPath + " --output="
					+ outputPath;
			Runtime rt = Runtime.getRuntime();
			try {
				rt.exec(new String[] { "bash", "-c", sortCommand }).waitFor();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println(inputPath);
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
			File inputFile = new File(inputPath);
			String outputPath = inputPath.replace(inputExtension,
					outputExtension);
			String sortCommand = "sort -S3G " + inputPath + " --output="
					+ outputPath;
			Runtime rt = Runtime.getRuntime();
			try {
				rt.exec(new String[] { "bash", "-c", sortCommand }).waitFor();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			inputFile.delete();
		}
		// note: when sorting an empty file, new file contains "null1"
	}
}
