package de.typology.parser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import de.typology.utils.IOHelper;

public class DGTTMMain {
	private static ArrayList<File> fileList;

	/**
	 * @author Martin Koerner
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
	}

	public static void run(String dgttmInputPath, String parsedOutputPath,
			String normalizedOutputPath, String dgttmLanguage)
					throws IOException {
		long startTime = System.currentTimeMillis();
		IOHelper.log("getting file list");
		fileList = IOHelper.getDirectory(new File(dgttmInputPath));

		DGTTMParser parser = new DGTTMParser(fileList, parsedOutputPath,
				dgttmLanguage);
		IOHelper.log("start parsing");
		parser.parse();
		IOHelper.log("parsing done");
		IOHelper.log("start cleanup");
		DGTTMNormalizer wn = new DGTTMNormalizer(parsedOutputPath,
				normalizedOutputPath);
		wn.normalize();
		IOHelper.log("cleanup done");
		IOHelper.log("generate indicator file");
		long endTime = System.currentTimeMillis();
		long time = (endTime - startTime) / 1000;
		IOHelper.strongLog("done normalizing: " + dgttmInputPath + ", time: "
				+ time + " seconds");
	}
}
