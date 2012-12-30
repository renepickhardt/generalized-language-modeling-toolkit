package de.typology.lexerParser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import de.typology.utils.IOHelper;

public class ReutersMain {
	private static ArrayList<File> fileList;

	/**
	 * @author Martin Koerner
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
	}

	public static void run(String reutersInputPath, String parsedOutputPath,
			String normalizedOutputPath) throws IOException {
		long startTime = System.currentTimeMillis();
		IOHelper.log("getting file list");
		fileList = IOHelper.getDirectory(new File(reutersInputPath));

		ReutersParser parser = new ReutersParser(fileList, parsedOutputPath);
		IOHelper.log("start parsing");
		parser.parse();
		IOHelper.log("parsing done");
		IOHelper.log("start cleanup");
		ReutersNormalizer wn = new ReutersNormalizer(parsedOutputPath,
				normalizedOutputPath);
		wn.normalize();
		IOHelper.log("cleanup done");
		IOHelper.log("generate indicator file");
		long endTime = System.currentTimeMillis();
		long time = (endTime - startTime) / 1000;
		IOHelper.strongLog("done normalizing: " + reutersInputPath + ", time: "
				+ time + " seconds");
	}
}
