package de.typology.testutils;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;

import org.apache.commons.io.FileUtils;

public class Resetter {

	public static HashSet<String> doNotDelete = new HashSet<String>();
	static {
		// add the files here that should not be deleted
		doNotDelete.add("testDataset.txt");
		doNotDelete.add("normalized.txt");
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Resetter.reset("testDataset/");

	}

	public static void reset(String path) {
		File file = new File(path);
		Resetter.reset(file);
	}

	public static void reset(File inputFile) {
		File[] files = inputFile.listFiles();
		for (File file : files) {
			if (!doNotDelete.contains(file.getName())) {
				try {
					if (file.isDirectory()) {
						FileUtils.deleteDirectory(file);
					} else {
						file.delete();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
