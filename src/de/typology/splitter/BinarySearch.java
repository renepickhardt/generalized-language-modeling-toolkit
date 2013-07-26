package de.typology.splitter;

public class BinarySearch {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		// String outputDirectory = Config.get().outputDirectory
		// + Config.get().inputDataSet;
		// IndexBuilder ib = new IndexBuilder();
		// String[] index = ib.deserializeIndex(outputDirectory + Config.get().indexName);
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

}
