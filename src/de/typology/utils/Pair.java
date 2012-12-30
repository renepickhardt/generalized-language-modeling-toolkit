package de.typology.utils;

/**
 * http://stackoverflow.com/questions/156275/what-is-the-equivalent-of-the-c-
 * pairl-r-in-java
 * 
 * 
 * @param <A>
 * @param <B>
 */
public class Pair<A, B> {
	private A first;
	private B second;

	public Pair(A first, B second) {
		super();
		this.first = first;
		this.second = second;
	}

	@Override
	public int hashCode() {
		int hashFirst = this.first != null ? this.first.hashCode() : 0;
		int hashSecond = this.second != null ? this.second.hashCode() : 0;

		return (hashFirst + hashSecond) * hashSecond + hashFirst;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Pair) {
			Pair otherPair = (Pair) other;
			return (this.first == otherPair.first || this.first != null
					&& otherPair.first != null
					&& this.first.equals(otherPair.first))
					&& (this.second == otherPair.second || this.second != null
							&& otherPair.second != null
							&& this.second.equals(otherPair.second));
		}

		return false;
	}

	@Override
	public String toString() {
		return "(" + this.first + ", " + this.second + ")";
	}

	public A getFirst() {
		return this.first;
	}

	public void setFirst(A first) {
		this.first = first;
	}

	public B getSecond() {
		return this.second;
	}

	public void setSecond(B second) {
		this.second = second;
	}
}