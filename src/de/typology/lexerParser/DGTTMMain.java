package de.typology.lexerParser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import de.typology.utils.Config;
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
		long startTime = System.currentTimeMillis();
		System.out.println("getting file list");
		fileList = IOHelper.getDirectory(new File(Config.get().DGTTMPath));

		DGTTMParser parser = new DGTTMParser(fileList,
				Config.get().parsedDGTTMOutputPath);
		System.out.println("start parsing");
		parser.parse();
		System.out.println("parsing done");
		System.out.println("start cleanup");
		DGTTMNormalizer wn = new DGTTMNormalizer(
				Config.get().parsedDGTTMOutputPath,
				Config.get().normalizedDGTTMOutputPath);
		wn.normalize();
		System.out.println("cleanup done");
		System.out.println("generate indicator file");
		long endTime = System.currentTimeMillis();
		long time = (endTime - startTime) / 1000;
		File done = new File(Config.get().normalizedDGTTMOutputPath + "IsDone."
				+ time + "s");
		done.createNewFile();
		System.out.println("done");
	}
}
