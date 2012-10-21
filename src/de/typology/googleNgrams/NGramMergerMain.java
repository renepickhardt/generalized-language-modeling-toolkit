package de.typology.googleNgrams;

import java.io.File;
import java.io.IOException;

import de.typology.utils.Config;

public class NGramMergerMain {

	/**
	 * @param args
	 * @throws IOException
	 * 
	 * @author Martin Koerner
	 */
	public static void main(String[] args) throws IOException {
		System.out.println("delete old output file");
		// SystemHelper
		// .runUnixCommand("rm " + Config.get().googleNgramsMergedPath);
		File inputDirectory = new File(Config.get().googleNgramsPath);
		File[] files = inputDirectory.listFiles();

		for (File file : files) {
			long startTime = System.currentTimeMillis();
			long endTime = 0;
			long sek = 0;
			System.out.println("start merging: " + file.getAbsolutePath());
			NGramMerger merger = new NGramMerger();
			String mergedOutputSub = file.getAbsolutePath().substring(
					file.getAbsolutePath().length() - 1,
					file.getAbsolutePath().length());
			String mergedOutputPath = Config.get().googleNgramsMergedPath
					+ mergedOutputSub + "gram-merged.txt";
			merger.merge(file.getAbsolutePath(), mergedOutputPath);
			System.out.println("merging done");
			System.out.println("generate indicator file");
			endTime = System.currentTimeMillis();
			sek = (endTime - startTime) / 1000;
			File done = new File(mergedOutputPath + "IsDone." + sek + "s");
			done.createNewFile();
		}
		System.out.println("done");
	}
}
