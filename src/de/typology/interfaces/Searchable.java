package de.typology.interfaces;

import java.util.HashMap;

public interface Searchable {
	int query(String q, String prefix, String match,
			int intermediateListLength, int k);

	public HashMap<String, Float> search(String q, String prefix,
			int numIntermediateLists, String match);

	public void saveWeights(int numQueries);
}
