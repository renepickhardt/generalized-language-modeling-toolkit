package de.typology.lexerParser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import de.typology.utils.Config;

public class EnronMain {
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
		fileList = new ArrayList<File>();
		getFileList(new File(Config.get().enronPath));

		EnronParser parser = new EnronParser(fileList,
				Config.get().parsedEnronOutputPath);
		System.out.println("start parsing");
		parser.parse();
		System.out.println("parsing done");
		System.out.println("start cleanup");
		EnronNormalizer wn = new EnronNormalizer(
				Config.get().parsedEnronOutputPath,
				Config.get().normalizedEnronOutputPath);
		wn.normalize();
		System.out.println("cleanup done");
		System.out.println("generate indicator file");
		long endTime = System.currentTimeMillis();
		long time = (endTime - startTime) / 1000;
		File done = new File(Config.get().normalizedEnronOutputPath + "IsDone."
				+ time + "s");
		done.createNewFile();
		System.out.println("done");
	}

	private static void getFileList(File f) {
		File[] files = f.listFiles();
		if (files != null) {
			for (File file : files) {
				if (file.isDirectory()) {
					getFileList(file);
				} else {
					fileList.add(file);
				}
			}
		}
	}

}
