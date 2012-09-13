package de.typology.trainers;

import java.util.ArrayList;
import java.util.List;

public class NGram {
	private String[] nGram;
	private int occurrences;

	public NGram(String[] nGram, int occurrences) {
		this.nGram = nGram;
		this.occurrences = occurrences;
	}

	public String[] getNGram() {
		return this.nGram;
	}

	public int getOccurrences() {
		return this.occurrences;
	}

	public List<Pair> getPairsWithEdgeType(int edgeType) {
		List<Pair> pairs = new ArrayList<Pair>();
		if (edgeType >= this.nGram.length) {
			return pairs;
		}
		int first = 0;
		for (int second = edgeType; second < this.nGram.length; first++, second++) {
			pairs.add(new Pair(this.nGram[first], this.nGram[second]));
		}
		return pairs;
	}

	@Override
	public String toString() {
		String result = "";
		for (String element : this.nGram) {
			result += element + " ";
		}
		result += "\t" + this.occurrences;
		return result;
	}
}
