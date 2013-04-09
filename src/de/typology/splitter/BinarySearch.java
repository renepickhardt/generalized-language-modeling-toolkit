package de.typology.splitter;


public class BinarySearch {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		// String outputDirectory = Config.get().outputDirectory
		// + Config.get().inputDataSet;
		// IndexBuilder ib = new IndexBuilder();
		// String[] index = ib.deserializeIndex(outputDirectory + "index.txt");
		// String[] result;
		// String key;
		// String prefix;
		// // key = "A";
		// // result = BinarySearch.rankPrefix(key, index);
		// // System.out.println(key + ":");
		// // for (String s : result) {
		// // System.out.println(s);
		// // }
		// System.out.println("----");
		// key = "";
		// prefix = "";
		// result = BinarySearch.rank(key, prefix, ".5gs", outputDirectory
		// + "ngrams/5gs/", index);
		// System.out.println(key + "-" + prefix + ":");
		// for (String s : result) {
		// System.out.println(s);
		// }
		// System.out.println("----");
		// key = "da";
		// prefix = "";
		// result = BinarySearch.rank(key, prefix, ".5gs", outputDirectory
		// + "ngrams/5gs/", index);
		// System.out.println(key + "-" + prefix + ":");
		// for (String s : result) {
		// System.out.println(s);
		// }
		// System.out.println("----");
		// prefix = "A";
		// result = BinarySearch.rank(key, prefix, ".5gs", outputDirectory
		// + "ngrams/5gs/", index);
		// System.out.println(key + "-" + prefix + ":");
		// for (String s : result) {
		// System.out.println(s);
		// }
		// System.out.println("----");
		// key = "";
		// result = BinarySearch.rank(key, prefix, ".5gs", outputDirectory
		// + "ngrams/5gs/", index);
		// System.out.println(key + "-" + prefix + ":");
		// for (String s : result) {
		// System.out.println(s);
		// }

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
			// return all files
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
	public static String rankWithAll(String key, String[] index) {
		String result;
		if (key.length() == 0) {
			// search for prefix only (e.g. 1grams)
			result = "all";
		} else {
			// search for key and prefix
			result = String.valueOf(BinarySearch.rank(key, index));
		}
		return result;
	}
}
