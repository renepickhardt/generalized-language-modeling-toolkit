package de.typology.parser;

import java.io.File;
import java.io.IOException;

import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public class GoogleMergerMain {

	/**
	 * @param args
	 * @throws IOException
	 * 
	 * @author Martin Koerner
	 */
	public static void main(String[] args) throws IOException {
		File dir = new File(Config.get().googleInputDirectory);
		String outputDirectory = Config.get().outputDirectory + "google/";
		new File(outputDirectory).mkdirs();
		for (File file : dir.listFiles()) {
			String dataSet = file.getName();
			new File(outputDirectory + dataSet).mkdirs();
			System.out.println(file.getAbsolutePath());
			GoogleMergerMain.run(file.getAbsolutePath(), outputDirectory
					+ dataSet + "/");
		}
	}

	public static void run(String googleInputPath, String outputPath)
			throws IOException {
		IOHelper.log("getting files for merging");
		File inputDirectory = new File(googleInputPath);
		File[] files = inputDirectory.listFiles();

		for (File file : files) {
			long startTime = System.currentTimeMillis();
			long endTime = 0;
			long sek = 0;
			IOHelper.log("start merging: " + file.getAbsolutePath());
			GoogleMerger merger = new GoogleMerger();
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
