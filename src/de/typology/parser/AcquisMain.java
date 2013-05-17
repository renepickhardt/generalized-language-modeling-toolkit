package de.typology.parser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import de.typology.utils.IOHelper;

public class AcquisMain {
	private static ArrayList<File> fileList;

	/**
	 * @author Martin Koerner
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
	}

	public static void run(String acquisInputPath, String parsedOutputPath,
			String normalizedOutputPath, String acquisLanguage)
			throws IOException {
		long startTime = System.currentTimeMillis();
		IOHelper.log("getting file list");
		fileList = IOHelper.getDirectory(new File(acquisInputPath));

		AcquisParser parser = new AcquisParser(fileList, parsedOutputPath,
				acquisLanguage);
		IOHelper.log("start parsing: " + acquisInputPath);
		parser.parse();
		IOHelper.log("parsing done");
		IOHelper.log("start cleanup");
		AcquisNormalizer wn = new AcquisNormalizer(parsedOutputPath,
				normalizedOutputPath);
		wn.normalize();
		IOHelper.log("cleanup done");
		IOHelper.log("generate indicator file");
		long endTime = System.currentTimeMillis();
		long time = (endTime - startTime) / 1000;
		IOHelper.strongLog("done normalizing: " + acquisInputPath + ", time: "
				+ time + " seconds");
	}
}
