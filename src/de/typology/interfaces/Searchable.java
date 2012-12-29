package de.typology.interfaces;

import java.util.HashMap;

public interface Searchable {
	int query(String q, String prefix, String match);

	public HashMap<String, Float> search(String q, String prefix, String match);

	public void saveWeights(int numQueries);

	void run(Searchable lts, int n, int topK, int joinLength);// Searchable
	// lts won't
	// work

	String getFileName();

	public String prepareQuery(String[] words, int n);
}
