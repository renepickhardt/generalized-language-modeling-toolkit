package de.typology.lexerParser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import de.typology.utils.IOHelper;

public class EnronMain {
	private static ArrayList<File> fileList;

	/**
	 * @author Martin Koerner
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
	}

	public static void run(String enronInputPath, String parsedOutputPath,
			String normalizedOutputPath) throws IOException {
		long startTime = System.currentTimeMillis();
		IOHelper.log("getting file list");
		fileList = IOHelper.getDirectory(new File(enronInputPath));

		EnronParser parser = new EnronParser(fileList, parsedOutputPath);
		IOHelper.log("start parsing");
		parser.parse();
		IOHelper.log("parsing done");
		IOHelper.log("start cleanup");
		EnronNormalizer wn = new EnronNormalizer(parsedOutputPath,
				normalizedOutputPath);
		wn.normalize();
		IOHelper.log("cleanup done");
		IOHelper.log("generate indicator file");
		long endTime = System.currentTimeMillis();
		long time = (endTime - startTime) / 1000;
		IOHelper.strongLog("done normalizing: " + enronInputPath + ", time: "
				+ time + " seconds");
	}
}
