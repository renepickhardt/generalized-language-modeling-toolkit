package de.typology.utilsOld;

import java.io.File;
import java.util.ArrayList;

public class FileTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ArrayList<File> files = new ArrayList<File>();
		for (int i = 0; i < Config.get().fileSizeThreashhold; i++) {
			System.out.println(i);
			files.add(new File("/home/martin/test" + i));
		}
	}
}
