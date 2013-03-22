package de.typology.splitter;

import java.io.File;

import de.typology.utils.Config;

public class BinarySearch {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// String[] test = { "ab", "dr", "te", "yr" };
		// System.out.println("$ads: " + BinarySearch.rank("$ads", test) +
		// "=0");
		// System.out.println("00: " + BinarySearch.rank("00", test) + "=0");
		// System.out.println("aabc: " + BinarySearch.rank("aabc", test) +
		// "=0");
		// System.out.println("ab: " + BinarySearch.rank("ab", test) + "=0");
		// System.out.println("aba: " + BinarySearch.rank("aba", test) + "=0");
		// System.out.println("abz: " + BinarySearch.rank("abz", test) + "=0");
		// System.out.println("abA: " + BinarySearch.rank("abA", test) + "=0");
		// System.out.println("abZ: " + BinarySearch.rank("abZ", test) + "=0");
		// System.out.println("dqz: " + BinarySearch.rank("dqz", test) + "=0");
		// System.out.println("ds: " + BinarySearch.rank("ds", test) + "=1");
		// System.out.println("er: " + BinarySearch.rank("er", test) + "=1");
		// System.out.println("td: " + BinarySearch.rank("td", test) + "=1");
		// System.out.println("yqz: " + BinarySearch.rank("yqz", test) + "=2");
		// System.out.println("yr: " + BinarySearch.rank("yr", test) + "=3");
		// System.out.println("yra: " + BinarySearch.rank("yra", test) + "=3");
		// System.out.println("za: " + BinarySearch.rank("za", test) + "=3");
		String outputDirectory = Config.get().outputDirectory
				+ Config.get().inputDataSet;
		IndexBuilder ib = new IndexBuilder();
		String[] index = ib.deserializeIndex(outputDirectory + "index.txt");

		System.out.println(BinarySearch.rank("Test", ".1gs", outputDirectory
				+ "ngrams/1gs/", index));
		System.out.println(BinarySearch.rank("Test", null, "s", ".1gs",
				outputDirectory + "ngrams/1gs/", index));

		System.out.println(BinarySearch.rank("de", "Art", "s", ".5gs",
				outputDirectory + "ngrams/5gs/", index));

	}

	/**
	 * Binary search returning the best fitting position for String key in
	 * String[] a.
	 * <p>
	 * Warning: May not behave like the standard binary search.
	 * <p>
	 * String[] a has to be sorted.
	 */
	public static int rank(String key, String[] index) {
		int lo = 0;
		int hi = index.length - 1;
		while (lo <= hi) {
			int mid = lo + (hi - lo) / 2;
			if (key.compareTo(index[mid]) < 0) {
				hi = mid - 1;
			} else if (key.compareTo(index[mid]) > 0) {
				lo = mid + 1;
			} else {
				return mid;
			}
		}
		// the following return statement is not the standard return result for
		// binary search
		return (lo + hi) / 2;
	}

	/**
	 * Binary search returning the according file for String key in String[]
	 * directory based on String[] index.
	 * <p>
	 * Warning: May not behave like the standard binary search.
	 * 
	 */
	public static String rank(String key, String extension, String directory,
			String[] index) {
		String name = String.valueOf(BinarySearch.rank(key, index));
		File firstFile = new File(directory + name + extension);
		if (firstFile.exists()) {
			return firstFile.getName();
		} else {
			return null;
		}
	}

	/**
	 * Binary search returning the according file for Strings firstKey and
	 * secondKey in String[] directory based on String[] index. The file name is
	 * separated by String separator
	 * <p>
	 * Warning: May not behave like the standard binary search.
	 * 
	 */
	public static String rank(String firstKey, String prefix, String separator,
			String extension, String directory, String[] index) {
		String name = String.valueOf(BinarySearch.rank(firstKey, index));
		File firstFile = new File(directory + name + extension);
		if (firstFile.exists()) {
			// there was no second split for this file
			return firstFile.getName();
		} else {
			// either there is a second split or file doesn't exist at all

			// TODO: design decision: how do we handle second split files?
			// aggregate or join results in sql?
			name += separator + BinarySearch.rank(prefix, index);
			File secondFile = new File(directory + name + extension);
			if (secondFile.exists()) {
				return secondFile.getName();
			} else {
				return null;
			}
		}
	}
}
