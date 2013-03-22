package de.typology.splitter;

import java.io.File;
import java.util.ArrayList;

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
		String[] result;
		String key;
		String prefix;
		// key = "A";
		// result = BinarySearch.rankPrefix(key, index);
		// System.out.println(key + ":");
		// for (String s : result) {
		// System.out.println(s);
		// }
		System.out.println("----");
		key = "";
		prefix = "";
		result = BinarySearch.rank(key, prefix, "s", ".5gs", outputDirectory
				+ "ngrams/5gs/", index);
		System.out.println(key + "-" + prefix + ":");
		for (String s : result) {
			System.out.println(s);
		}
		System.out.println("----");
		key = "da";
		prefix = "";
		result = BinarySearch.rank(key, prefix, "s", ".5gs", outputDirectory
				+ "ngrams/5gs/", index);
		System.out.println(key + "-" + prefix + ":");
		for (String s : result) {
			System.out.println(s);
		}
		System.out.println("----");
		prefix = "A";
		result = BinarySearch.rank(key, prefix, "s", ".5gs", outputDirectory
				+ "ngrams/5gs/", index);
		System.out.println(key + "-" + prefix + ":");
		for (String s : result) {
			System.out.println(s);
		}
		System.out.println("----");
		key = "";
		result = BinarySearch.rank(key, prefix, "s", ".5gs", outputDirectory
				+ "ngrams/5gs/", index);
		System.out.println(key + "-" + prefix + ":");
		for (String s : result) {
			System.out.println(s);
		}

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
	 * Binary search returning the best fitting position for String key in
	 * String[] a.
	 * <p>
	 * Warning: May not behave like the standard binary search.
	 * <p>
	 * String[] a has to be sorted.
	 */
	public static int[] rankPrefix(String prefix, String[] index) {
		int[] result;
		// prefix is empty
		if (prefix.length() == 0) {
			result = new int[index.length];
			for (int i = 0; i < index.length; i++) {
				result[i] = i;
			}
			return result;
		}
		// prefix is not empty

		// get first and last possible file
		int firstFile = BinarySearch.rank(prefix, index);
		String lastName = prefix + Character.MAX_VALUE;

		int lastFile = BinarySearch.rank(lastName, index);
		// System.out.println("prefix:" + prefix);
		// System.out.println("firstFile: " + firstFile);
		// System.out.println("upperBound: " + lastName);
		// System.out.println("upperBoundFile: " + lastFile);
		result = new int[lastFile - firstFile + 1];
		int file = firstFile;
		for (int i = 0; i < result.length; i++) {
			result[i] = file;
			file++;
		}
		return result;
	}

	/**
	 * Binary search returning the according file for Strings key and prefix in
	 * String[] directory based on String[] index. The file name is separated by
	 * String separator
	 * <p>
	 * 
	 */
	public static String[] rank(String key, String prefix, String separator,
			String extension, String directory, String[] index) {
		String[] result;
		File file;
		ArrayList<String> tempResult;
		if (key.length() == 0) {
			// search for prefix only (e.g. 1grams)
			int[] prefixRanks = BinarySearch.rankPrefix(prefix, index);
			tempResult = new ArrayList<String>();

			for (int firstPart : prefixRanks) {

				file = new File(directory + firstPart + extension);
				if (file.exists()) {
					tempResult.add(file.getName());
				} else {
					// second for loop catches all second split files
					for (int secondPart = 0; secondPart < index.length; secondPart++) {
						file = new File(directory + firstPart + separator
								+ secondPart + extension);
						if (file.exists()) {
							tempResult.add(file.getName());
						}
					}
				}
			}
		} else {
			// search for key and prefix

			// first search for key
			String firstPart = String.valueOf(BinarySearch.rank(key, index));
			file = new File(directory + firstPart + extension);
			if (file.exists()) {
				// there is no second split on this file
				result = new String[1];
				result[0] = firstPart + extension;
				return result;
			} else {
				// either there is a second split or file doesn't exist at all
				int[] prefixRanks = BinarySearch.rankPrefix(prefix, index);
				tempResult = new ArrayList<String>();
				for (int secondPart : prefixRanks) {
					file = new File(directory + firstPart + separator
							+ secondPart + extension);
					if (file.exists()) {
						tempResult.add(file.getName());
					}
				}
			}
		}
		result = new String[tempResult.size()];
		for (int i = 0; i < result.length; i++) {
			result[i] = tempResult.get(i);
		}
		return result;

	}
}
