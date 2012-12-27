package de.typology.googleNGrams;

import java.io.File;
import java.io.IOException;

import de.typology.utils.IOHelper;

public class NGramMergerMain {

	/**
	 * @param args
	 * @throws IOException
	 * 
	 * @author Martin Koerner
	 */
	public static void main(String[] args) throws IOException {
	}

	public static void run(String googleInputPath, String outputPath)
			throws IOException {
		File inputDirectory = new File(googleInputPath);
		File[] files = inputDirectory.listFiles();

		for (File file : files) {
			long startTime = System.currentTimeMillis();
			long endTime = 0;
			long sek = 0;
			IOHelper.log("start merging: " + file.getAbsolutePath());
			NGramMerger merger = new NGramMerger();
			String mergedOutputSub = file.getAbsolutePath().substring(
					file.getAbsolutePath().length() - 1,
					file.getAbsolutePath().length());
			String mergedOutputPath = outputPath + mergedOutputSub
					+ "gram-merged.txt";
			merger.merge(file.getAbsolutePath(), mergedOutputPath);
			IOHelper.log("merging done");
			IOHelper.log("generate indicator file");
			endTime = System.currentTimeMillis();
			sek = (endTime - startTime) / 1000;
			IOHelper.strongLog("done merging: " + file.getAbsolutePath()
					+ ", time: " + sek + " seconds");
		}
		IOHelper.log("done");
	}
}
