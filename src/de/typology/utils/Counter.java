package de.typology.utils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

public class Counter {

	public static long countLinesInDirectory(File directory) {
		long totalCount = 0;
		for (File file : directory.listFiles()) {
			totalCount += countLines(file);
		}
		return totalCount;
	}

	// derived from:
	// http://stackoverflow.com/questions/453018/number-of-lines-in-a-file-in-java
	public static long countLines(File file) {
		InputStream is;
		try {
			is = new BufferedInputStream(new FileInputStream(file));
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

	private static int columnNumberStartZero;
	private static File directory;
	private static long currentCountForDirectory;

	public static long countColumnCountsInDirectory(int columnNumberStartZero,
			File directory) {
		if (columnNumberStartZero == Counter.columnNumberStartZero
				&& directory.equals(Counter.directory)) {
			return Counter.currentCountForDirectory;
		} else {
			long totalCount = 0;
			for (File file : directory.listFiles()) {
				totalCount += countColumnCounts(columnNumberStartZero, file);
			}
			Counter.columnNumberStartZero = columnNumberStartZero;
			Counter.currentCountForDirectory = totalCount;
			Counter.directory = directory;
			return totalCount;
		}
	}

	public static long countColumnCounts(int columnNumberStartZero, File file) {
		long totalCount = 0;
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			try {
				String line;
				String[] lineSplit;
				while ((line = br.readLine()) != null) {
					lineSplit = line.split("\t");
					totalCount += Long
							.parseLong(lineSplit[columnNumberStartZero]);
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
	 * used for aggregating the counts in a directory
	 * 
	 * @param count
	 * @param directory
	 * @return
	 */
	public static long aggregateCountsInDirectory(File directory) {
		long totalCount = 0;
		for (File file : directory.listFiles()) {
			totalCount += aggregateCounts(file);
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
	public static long aggregateCounts(File file) {
		long totalCount = 0;
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			try {
				String line;
				String[] lineSplit;
				while ((line = br.readLine()) != null) {
					// TODO remove this or make it pretty
					if (line.startsWith("<fs>")) {
						continue;
					}
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
	 * @param directory
	 * @return
	 */
	public static long countCountsInDirectory(int count, File directory,
			String skipSequence) {
		long totalCount = 0;
		for (File file : directory.listFiles()) {
			if (!file.getName().contains("-split")) {
				totalCount += countCounts(count, file, skipSequence);
			}
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
	public static long countCounts(int count, File file, String skipSequence) {
		long totalCount = 0;
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			try {
				String line;
				String[] lineSplit;
				while ((line = br.readLine()) != null) {
					if (line.startsWith("<fs>")) {
						continue;
					}
					// FIXME: put the delimiter to a global config file or at
					// least as a constant
					lineSplit = line.split("\t");
					long currentCount;
					if (lineSplit.length == 1) {
						currentCount = Long.parseLong(lineSplit[0]);
					} else {
						currentCount = Long.parseLong(lineSplit[1]);
					}
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