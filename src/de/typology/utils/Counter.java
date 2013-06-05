package de.typology.utils;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class Counter {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out
				.println(Counter
						.countLines("/home/martin/typoeval/out/wiki/en/glm-normalized/1/692.1_split"));

	}

	// derived from:
	// http://stackoverflow.com/questions/453018/number-of-lines-in-a-file-in-java
	public static long countLines(String filename) {
		InputStream is;
		try {
			is = new BufferedInputStream(new FileInputStream(filename));
			try {
				try {
					byte[] c = new byte[1024];
					long count = 0;
					int readChars = 0;
					boolean empty = true;
					while ((readChars = is.read(c)) != -1) {
						empty = false;
						for (int i = 0; i < readChars; ++i) {
							if (c[i] == '\n') {
								++count;
							}
						}
					}
					return count == 0 && !empty ? 1 : count;
				} finally {
					is.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return 0;
	}
}
