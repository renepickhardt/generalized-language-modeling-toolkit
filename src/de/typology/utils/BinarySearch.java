package de.typology.utils;

public class BinarySearch {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String[] test = { "ab", "dr", "te", "yr" };
		System.out.println("$ads: " + BinarySearch.rank("$ads", test) + "=0");
		System.out.println("00: " + BinarySearch.rank("00", test) + "=0");
		System.out.println("aabc: " + BinarySearch.rank("aabc", test) + "=0");
		System.out.println("ab: " + BinarySearch.rank("ab", test) + "=0");
		System.out.println("aba: " + BinarySearch.rank("aba", test) + "=0");
		System.out.println("abz: " + BinarySearch.rank("abz", test) + "=0");
		System.out.println("abA: " + BinarySearch.rank("abA", test) + "=0");
		System.out.println("abZ: " + BinarySearch.rank("abZ", test) + "=0");
		System.out.println("dqz: " + BinarySearch.rank("dqz", test) + "=0");
		System.out.println("ds: " + BinarySearch.rank("ds", test) + "=1");
		System.out.println("er: " + BinarySearch.rank("er", test) + "=1");
		System.out.println("td: " + BinarySearch.rank("td", test) + "=1");
		System.out.println("yqz: " + BinarySearch.rank("yqz", test) + "=2");
		System.out.println("yr: " + BinarySearch.rank("yr", test) + "=3");
		System.out.println("yra: " + BinarySearch.rank("yra", test) + "=3");
		System.out.println("za: " + BinarySearch.rank("za", test) + "=3");

	}

	/**
	 * Binary search returning the best fitting position for String key in
	 * String[] a.
	 * <p>
	 * Warning: May not behave like the standard binary search.
	 * <p>
	 * String[] a has to be sorted.
	 */
	public static int rank(String key, String[] a) {
		int lo = 0;
		int hi = a.length - 1;
		while (lo <= hi) {
			int mid = lo + (hi - lo) / 2;
			if (key.compareTo(a[mid]) < 0) {
				hi = mid - 1;
			} else if (key.compareTo(a[mid]) > 0) {
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
