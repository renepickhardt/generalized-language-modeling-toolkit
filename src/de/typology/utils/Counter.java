package de.typology.utils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
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
						.countLinesInDirectory("/home/martin/typoeval/out/wiki/en/glm-normalized/1/"));
		System.out
				.println(Counter
						.countLines("/home/martin/typoeval/out/wiki/en/glm-normalized/1/692.1_split"));

	}

	public static long countLinesInDirectory(String directoryName) {
		long totalCount = 0;
		for (File file : new File(directoryName).listFiles()) {
			totalCount += countLines(file.getAbsolutePath());
		}
		return totalCount;
	}

	// derived from:
	// http://stackoverflow.com/questions/453018/number-of-lines-in-a-file-in-java
	public static long countLines(String fileName) {
		InputStream is;
		try {
			is = new BufferedInputStream(new FileInputStream(fileName));
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

	private static String directoryName;
	private static long currentCountForDirectory;

	public static long countColumnCountsInDirectory(String directoryName) {
		if (directoryName.equals(Counter.directoryName)) {
			return Counter.currentCountForDirectory;
		} else {
			long totalCount = 0;
			for (File file : new File(directoryName).listFiles()) {
				totalCount += countColumnCounts(file.getAbsolutePath());
			}
			Counter.currentCountForDirectory = totalCount;
			Counter.directoryName = directoryName;
			return totalCount;
		}
	}

	public static long countColumnCounts(String fileName) {
		long totalCount = 0;
		BufferedReader br = IOHelper.openReadFile(fileName,
				Config.get().memoryLimitForReadingFiles);
		try {
			try {
				String line;
				String[] lineSplit;
				while ((line = br.readLine()) != null) {
					lineSplit = line.split("\t");
					totalCount += Long
							.parseLong(lineSplit[lineSplit.length - 1]);
				}
			} finally {
				br.close();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return totalCount;
	}

	/**
	 * used for calculating the count of counts in smoothing methods
	 * 
	 * @param count
	 * @param directoryName
	 * @return
	 */
	public static long countCountsInDirectory(int count, String directoryName) {
		long totalCount = 0;
		for (File file : new File(directoryName).listFiles()) {
			totalCount += countCounts(count, file.getAbsolutePath());
		}
		return totalCount;
	}

	/**
	 * used for calculating the count of counts in smoothing methods
	 * 
	 * @param count
	 * @param directoryName
	 * @return
	 */
	public static long countCounts(int count, String fileName) {
		long totalCount = 0;
		BufferedReader br = IOHelper.openReadFile(fileName,
				Config.get().memoryLimitForReadingFiles);
		try {
			try {
				String line;
				String[] lineSplit;
				while ((line = br.readLine()) != null) {
					lineSplit = line.split("\t");
					long currentCount = Long
							.parseLong(lineSplit[lineSplit.length - 1]);
					if (count == currentCount && !lineSplit[0].equals("<fs>")) {
						totalCount += 1;
					}
				}
			} finally {
				br.close();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return totalCount;
	}
}
