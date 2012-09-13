package de.typology.trainers;

public class Pair {
	private String first;
	private String second;

	public Pair(String first, String second) {
		this.first = first;
		this.second = second;
	}

	public String getFirst() {
		return this.first;
	}

	public String getSecond() {
		return this.second;
	}

	@Override
	public String toString() {
		return "(" + this.first + ", " + this.second + ")";
	}

	@Override
	public boolean equals(Object obj) {
		if (this.first.equals(((Pair) obj).getFirst())
				&& this.second.equals(((Pair) obj).getSecond())) {
			return true;
		} else {
			return false;
		}
	}
}
