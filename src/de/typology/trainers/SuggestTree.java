package de.typology.trainers;

/*
 * Copyright 2011-2012 Nicolai Diethelm
 *
 * Modified by Martin Koerner
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


import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

/**
 * An efficient data structure for rank-ordered autocomplete suggestions.
 * <p>
 * Class Pair was added to support storing of rank scores.
 *
 * @param <V>
 * the type of rank values
 *
 * @version 26 November 2012
 */
public class SuggestTree<V> {

	private Node<V> root = null;
	private int nodeCount = 0;
	private int arrayCount = 0;
	private int totalArrayLength = 0;

	/**
	 * Creates an empty tree.
	 */
	public SuggestTree() {
	}

	/**
	 * Builds a tree with the specified suggestion strings and the specified
	 * ranking that returns the top {@code k} autocomplete suggestions for a
	 * prefix. The old tree is left intact while the new tree is built, allowing
	 * concurrent calls to {@code getBestSuggestions} to access the old tree.
	 *
	 * @param m
	 * a map containing the suggestions and their rank values
	 * @param c
	 * a rank value comparator whose {@code compare} method returns a
	 * negative integer, zero, or a positive integer as the first
	 * value means a lower rank than, the same rank as, or a higher
	 * rank than the second
	 * @param k
	 * maximum length for the suggestion list of a node
	 * @throws RuntimeException
	 * if any of the specified suggestions are an empty string
	 */
	public void build(Map<String, V> m, final Comparator<V> c, int k) {
		@SuppressWarnings("unchecked")
		Pair<V>[] suggestions = new Pair[m.size()];
		int count = 0;
		for (Map.Entry<String, V> e : m.entrySet()) {
			suggestions[count] = new Pair<V>(e.getKey(), e.getValue());
			count++;
		}
		Pair<V>[] pairs = suggestions.clone();
		Arrays.sort(suggestions);
		Arrays.sort(pairs, new Comparator<Pair<V>>() {
			@Override
			public int compare(Pair<V> e1, Pair<V> e2) {
				return c.compare(e2.score, e1.score);
			}
		});
		SuggestTree<V> t = new SuggestTree<V>();
		t.buildTST(suggestions, 0, suggestions.length);
		t.allocateArrays(t.root, k);
		for (Pair<V> e : pairs) {
			t.appendToLists(e, k);
		}
		this.root = t.root;
		this.nodeCount = t.nodeCount;
		this.arrayCount = t.arrayCount;
		this.totalArrayLength = t.totalArrayLength;
	}

	private void buildTST(Pair<V>[] suggestions, int min, int max) {
		if (min < max) {
			int mid = (min + max) / 2;
			this.root = this.insert(suggestions[mid], 0, this.root);
			this.buildTST(suggestions, min, mid);
			this.buildTST(suggestions, mid + 1, max);
		}
	}

	private Node<V> insert(Pair<V> suggestion, int i, Node<V> n) {
		if (n == null) {
			n = new Node<V>(suggestion, suggestion.getString().length(), null,
					1);
			this.nodeCount++;
		} else if (suggestion.getString().charAt(i) < n.first.getString()
				.charAt(i)) {
			n.left = this.insert(suggestion, i, n.left);
		} else if (suggestion.getString().charAt(i) > n.first.getString()
				.charAt(i)) {
			n.right = this.insert(suggestion, i, n.right);
		} else {
			for (i++; i < n.end; i++) {
				if (i == suggestion.getString().length()
						|| suggestion.getString().charAt(i) != n.first
						.getString().charAt(i)) {
					n.down = new Node<V>(n.first, n.end, n.down, n.count);
					n.end = i;
					this.nodeCount++;
					break;
				}
			}
			n.count++;
			if (i < suggestion.getString().length()) {
				n.down = this.insert(suggestion, i, n.down);
			}
		}
		return n;
	}

	@SuppressWarnings("unchecked")
	private void allocateArrays(Node<V> n, int k) {
		if (n != null) {
			int size = Math.min(n.count, k) - 1;
			if (size > 0) {
				n.tail = new Pair[size];
				this.arrayCount++;
				this.totalArrayLength += size;
			}
			n.count = 0;
			this.allocateArrays(n.left, k);
			this.allocateArrays(n.down, k);
			this.allocateArrays(n.right, k);
		}
	}

	private void appendToLists(Pair<V> suggestion, int k) {
		int i = 0;
		Node<V> n = this.root;
		while (true) {
			if (suggestion.getString().charAt(i) < n.first.getString()
					.charAt(i)) {
				n = n.left;
			} else if (suggestion.getString().charAt(i) > n.first.getString()
					.charAt(i)) {
				n = n.right;
			} else {
				if (n.count == 0) {
					n.first = suggestion;
					n.count = 1;
				} else if (n.count < k) {
					n.tail[n.count - 1] = suggestion;
					n.count++;
				}
				i = n.end;
				if (i < suggestion.getString().length()) {
					n = n.down;
				} else {
					return;
				}
			}
		}
	}

	/**
	 * Makes the tree empty.
	 */
	public void clear() {
		this.root = null;
		this.nodeCount = this.arrayCount = this.totalArrayLength = 0;
	}

	/**
	 * Returns the highest ranking suggestions that start with the specified
	 * prefix, or returns {@code null} if there is no such suggestion in the
	 * tree.
	 *
	 * @param prefix
	 * the prefix for which to return autocomplete suggestions
	 * @throws RuntimeException
	 * if the specified prefix is an empty string
	 */
	public Node<V> getBestSuggestions(String prefix) {
		int i = 0;
		Node<V> n = this.root;
		while (n != null) {
			if (prefix.charAt(i) < n.first.getString().charAt(i)) {
				n = n.left;
			} else if (prefix.charAt(i) > n.first.getString().charAt(i)) {
				n = n.right;
			} else {
				for (i++; i < n.end && i < prefix.length(); i++) {
					if (prefix.charAt(i) != n.first.getString().charAt(i)) {
						return null;
					}
				}
				if (i < prefix.length()) {
					n = n.down;
				} else {
					return n;
				}
			}
		}
		return null;
	}

	/**
	 * Returns the number of nodes in the tree, and the number and total length
	 * of the arrays used to store the tail of the suggestion lists.
	 */
	public int[] size() {
		return new int[] { this.nodeCount, this.arrayCount,
				this.totalArrayLength };
	}

	/**
	 * A tree node with a rank-ordered suggestion list.
	 */
	public static final class Node<V> {

		private Node<V> left = null;
		private Node<V> right = null;
		private Pair<V> first;
		private Pair<V>[] tail = null;
		private int count;
		private int end;
		private Node<V> down;

		private Node(Pair<V> first, int end, Node<V> down, int count) {
			this.first = first;
			this.end = end;
			this.down = down;
			this.count = count;
		}

		/**
		 * Returns the suggestion at the specified position in the list.
		 *
		 * @param index
		 * position of the suggestion to return (the first suggestion
		 * is at index 0)
		 * @throws RuntimeException
		 * if the specified index is negative or not less than the
		 * list length
		 */
		public Pair<V> getSuggestion(int index) {
			return index == 0 ? this.first : this.tail[index - 1];
		}

		/**
		 * Returns the number of suggestions in the list.
		 */
		public int listLength() {
			return this.count;
		}
	}

	/**
	 * this class was added to support storing of rank scores
	 */
	public static final class Pair<V> implements Comparable<V> {
		private String string;
		private V score;

		public Pair(String word, V score) {
			this.string = word;
			this.score = score;
		}

		public String getString() {
			return this.string;
		}

		public V getScore() {
			return this.score;
		}

		@SuppressWarnings("unchecked")
		@Override
		public int compareTo(Object o) {
			return this.getString().compareTo(((Pair<V>) o).getString());
		}
	}
}