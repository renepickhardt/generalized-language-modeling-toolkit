package de.typology.trainers;

import java.util.ArrayList;
import java.util.List;

public class NGram {
	private String[] nGram;
	private int length;

	public NGram(int length, String[] nGram) {
		this.length = length;
		this.nGram = nGram;
	}

	public String[] getNGram() {
		return this.nGram;
	}

	public int getLength() {
		return this.length;
	}

	public List<Pair> getPairsWithEdgeType(int edgeType) {
		List<Pair> pairs = new ArrayList<Pair>();
		if (edgeType >= this.length) {
			return pairs;
		}
		int first = 0;
		for (int second = edgeType; second < this.length; first++, second++) {
			pairs.add(new Pair(this.nGram[first], this.nGram[second]));
		}
		return pairs;
	}
}
