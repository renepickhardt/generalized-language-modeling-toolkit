package de.typology.db.optimized;

/*
 * Copyright 2011-2012 Nicolai Diethelm
 *
 * This software is free software. You can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

//TODO: nice data structure but it forgets the ranking values. After runtime tests are alright extend data structure to remember doubles within suggestionlist
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

/**
 * An efficient index data structure for rank-ordered autocomplete suggestions.
 * For any given prefix, it provides fast access to the top <i>k</i> suggestions
 * that start with that prefix.
 * <p>
 * The structure is based on a balanced ternary search tree of suggestions where
 * nodes (prefixes) with the same completions are compressed into one node and
 * where each node holds a rank-ordered list of references to the top <i>k</i>
 * suggestions that start with the corresponding prefix or prefixes. Because for
 * each suggestion inserted into the tree at most one new node is added and at
 * most one existing node is split into two nodes, the number of nodes in the
 * tree is always less than twice the number of suggestions. And because, in
 * practice, only a few internal nodes hold a long suggestion list, the average
 * length of the suggestion lists in the tree is very small.
 * <p>
 * The character sequence of a node is not stored explicitly at the node, but is
 * read up to a stored ending position from the first suggestion in the
 * suggestion list of the node. The reference to this first suggestion is stored
 * at the node itself while the references to the other suggestions (if the list
 * consists of more than one suggestion) are stored in an array.
 * <p>
 * Searching for the suggestion list for a prefix of length <i>p</i> in a tree
 * with <i>n</i> suggestions requires at most log<sub>2</sub>(<i>n</i>) +
 * <i>p</i> character comparisons. Roughly speaking, each comparison either
 * consumes one character of the prefix or cuts the search space in half.
 * 
 * @param <V>
 *            the type of rank values by which the suggestions are ordered
 * 
 * @author Nicolai Diethelm
 * @version 25 May 2012
 */
public class SuggestTree<V> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * A list of autocomplete suggestions. The list is "safe" in that it will
	 * not be affected if the backing tree gets rebuilt.
	 */
	public interface SuggestionList {

		/**
		 * Returns the suggestion at the specified position in this list. The
		 * first suggestion is at index 0, the second at index 1, and so on.
		 * 
		 * @param index
		 *            position of the suggestion to return
		 * @return the suggestion at the specified position in this list
		 * @throws IndexOutOfBoundsException
		 *             if {@code index} is negative or not less than the length
		 *             of this list
		 */
		String get(int index);

		/**
		 * Returns the number of suggestions in this list.
		 * 
		 * @return the number of suggestions in this list
		 */
		int length();
	}

	private static class Node implements SuggestionList {

		String[] list;
		String first;
		int end;
		int count;
		Node left, mid, right;

		@Override
		public String get(int index) {
			return index == 0 ? this.first : this.list[index - 1];
		}

		@Override
		public int length() {
			return this.count + 1;
		}
	}

	private static final SuggestionList EMPTY_LIST = new SuggestionList() {

		@Override
		public String get(int index) {
			throw new IndexOutOfBoundsException();
		}

		@Override
		public int length() {
			return 0;
		}
	};

	private static final String[] EMPTY_ARRAY = new String[0];
	private final int maxArrayLength;
	private final Comparator<Map.Entry<String, V>> rankComparator;
	private Node root, newRoot;
	private int nodeCount, arrayCount, totalArrayLength;

	/**
	 * Creates a tree that returns the top {@code k} autocomplete suggestions
	 * according to the specified comparator. The best-ranking suggestion is the
	 * one with the <em>least</em> rank value with respect to the ordering
	 * imposed by the comparator. Ties between suggestions with equal rank
	 * values are broken by the order of the map the tree is built from.
	 * 
	 * @param k
	 *            the maximum number of autocomplete suggestions that will be
	 *            returned for a given prefix
	 * @param comparator
	 *            the comparator that will be used by the {@code build} method
	 *            to compare the rank values of the suggestions
	 * @throws IllegalArgumentException
	 *             if {@code k} is less than 1 or the specified comparator is
	 *             {@code null}
	 */
	public SuggestTree(int k, final Comparator<V> comparator) {
		if (k < 1 || comparator == null) {
			throw new IllegalArgumentException();
		}
		this.maxArrayLength = k - 1;
		this.rankComparator = new Comparator<Map.Entry<String, V>>() {

			@Override
			public int compare(Map.Entry<String, V> e1, Map.Entry<String, V> e2) {
				return comparator.compare(e1.getValue(), e2.getValue());
			}
		};
	}

	/**
	 * Builds or rebuilds this tree from the specified map of suggestion strings
	 * and associated rank values. If the map is modified while this operation
	 * is in progress, the behavior of the tree is undefined.
	 * 
	 * @param map
	 *            the map of suggestion strings and associated rank values
	 * @throws ClassCastException
	 *             if any of the suggestions are not a string or the type of a
	 *             rank value is incompatible with the tree's comparator
	 * @throws IllegalArgumentException
	 *             if any of the suggestions are an empty string
	 * @throws NullPointerException
	 *             if the map or any of the suggestions are {@code null}, or if
	 *             any of the rank values are {@code null} and the tree's
	 *             comparator does not permit {@code null} values
	 */
	public void build(Map<String, V> map) {
		this.root = this.newRoot = null;
		this.nodeCount = this.arrayCount = this.totalArrayLength = 0;
		this.buildTernarySearchTree(map);
		this.buildSuggestionLists(map);
		this.root = this.newRoot;
	}

	/**
	 * Returns a rank-ordered list of the top <i>k</i> suggestions in this tree
	 * that start with the specified prefix. If the tree contains no suggestion
	 * with the prefix or this method is called while the tree is rebuilt, an
	 * empty list is returned.
	 * 
	 * @param prefix
	 *            the prefix for which to return the best-ranking autocomplete
	 *            suggestions
	 * @return a rank-ordered list of the top <i>k</i> suggestions in this tree
	 *         that start with the specified prefix
	 * @throws IllegalArgumentException
	 *             if the specified prefix is an empty string
	 * @throws NullPointerException
	 *             if the specified prefix is {@code null}
	 */
	public SuggestionList getBestSuggestions(String prefix) {
		if (prefix.isEmpty()) {
			return this.root;
		}
		// throw new IllegalArgumentException();
		int i = 0;
		Node n = this.root;
		while (n != null) {
			if (prefix.charAt(i) < n.first.charAt(i)) {
				n = n.left;
			} else if (prefix.charAt(i) > n.first.charAt(i)) {
				n = n.right;
			} else {
				while (++i < n.end) {
					if (i == prefix.length()) {
						return n;
					}
					if (prefix.charAt(i) != n.first.charAt(i)) {
						return EMPTY_LIST;
					}
				}
				if (i == prefix.length()) {
					return n;
				}
				n = n.mid;
			}
		}
		return EMPTY_LIST;
	}

	/**
	 * Returns information about the number of nodes and arrays in this tree and
	 * about the total length of the arrays.
	 * 
	 * @return information about the number of nodes and arrays in this tree and
	 *         about the total length of the arrays
	 */
	public String sizeInfo() {
		return "nodeCount=" + this.nodeCount + ", arrayCount="
				+ this.arrayCount + ", totalArrayLength="
				+ this.totalArrayLength;
	}

	private void buildTernarySearchTree(Map<String, V> map) {
		String[] suggestions = map.keySet().toArray(new String[0]);
		Arrays.sort(suggestions);
		if (suggestions.length > 0 && suggestions[0].isEmpty()) {
			throw new IllegalArgumentException();
		}
		this.buildBalancedTree(suggestions, 0, suggestions.length);
	}

	private void buildBalancedTree(String[] suggestions, int left, int right) {
		if (left < right) {
			int mid = (left + right) / 2;
			this.newRoot = this.insert(suggestions[mid], 0, this.newRoot);
			this.buildBalancedTree(suggestions, left, mid);
			this.buildBalancedTree(suggestions, mid + 1, right);
		}
	}

	private Node insert(String suggestion, int i, Node n) {
		if (n == null) {
			n = new Node();
			n.first = suggestion;
			n.end = suggestion.length();
			this.nodeCount++;
		} else if (suggestion.charAt(i) < n.first.charAt(i)) {
			n.left = this.insert(suggestion, i, n.left);
		} else if (suggestion.charAt(i) > n.first.charAt(i)) {
			n.right = this.insert(suggestion, i, n.right);
		} else {
			while (++i < n.end) {
				if (i == suggestion.length()
						|| suggestion.charAt(i) != n.first.charAt(i)) {
					Node mid = new Node();
					mid.first = n.first;
					mid.end = n.end;
					mid.count = n.count;
					mid.mid = n.mid;
					n.mid = mid;
					n.end = i;
					this.nodeCount++;
					break;
				}
			}
			if (n.count < this.maxArrayLength) {
				n.count++;
			}
			if (i < suggestion.length()) {
				n.mid = this.insert(suggestion, i, n.mid);
			}
		}
		return n;
	}

	private void buildSuggestionLists(Map<String, V> map) {
		Map.Entry<String, V>[] a = map.entrySet().toArray(new Map.Entry[0]);
		Arrays.sort(a, this.rankComparator);
		for (Map.Entry<String, V> e : a) {
			this.appendToLists(e.getKey());
		}
	}

	private void appendToLists(String suggestion) {
		int i = 0;
		Node n = this.newRoot;
		while (true) {
			if (suggestion.charAt(i) < n.first.charAt(i)) {
				n = n.left;
			} else if (suggestion.charAt(i) > n.first.charAt(i)) {
				n = n.right;
			} else {
				if (n.list == null) {
					n.first = suggestion;
					if (n.count == 0) {
						n.list = EMPTY_ARRAY;
					} else {
						n.list = new String[n.count];
						this.arrayCount++;
						this.totalArrayLength += n.count;
						n.count = 0;
					}
				} else if (n.count < this.maxArrayLength) {
					n.list[n.count++] = suggestion;
				}
				i = n.end;
				if (i == suggestion.length()) {
					return;
				}
				n = n.mid;
			}
		}
	}
}