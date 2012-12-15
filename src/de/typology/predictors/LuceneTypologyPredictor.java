package de.typology.predictors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import de.typology.interfaces.Predictable;
import de.typology.utils.Config;

public class LuceneTypologyPredictor implements Predictable {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		LuceneTypologyPredictor ltp = new LuceneTypologyPredictor(
				Config.get().indexPath);
		String[] fourGram = { "This", "is", "only", "a" };
		String[] result = ltp.predict(fourGram, "t");
		for (String s : result) {
			System.out.println(s);
		}
	}

	String indexPath;

	public LuceneTypologyPredictor(String indexPath) {
		this.indexPath = indexPath;
	}

	@Override
	public String[] predict(String[] fourGram, String prefix) {
		LuceneTypologySearcher lts = new LuceneTypologySearcher();
		ArrayList<HashMap<String, Float>> searchResults = new ArrayList<HashMap<String, Float>>();
		for (int edgeType = 1; edgeType < 5; edgeType++) {
			searchResults.add(lts.search(this.indexPath + edgeType + "/",
					prefix, fourGram[4 - edgeType]));
		}
		HashMap<String, Float> combinedSearchResults = new HashMap<String, Float>();

		// TODO externalize factors (order of relTypes in factors: [1,2,3,4])
		Float[] factors = { 5.0F, 2.0F, 2.0F, 0.0F };

		for (int i = 0; i < 4; i++) {
			for (Entry<String, Float> entry : searchResults.get(i).entrySet()) {
				if (combinedSearchResults.containsKey(entry.getKey())) {
					combinedSearchResults.put(entry.getKey(),
							combinedSearchResults.get(entry.getKey())
									+ factors[i] * entry.getValue());
				} else {
					combinedSearchResults.put(entry.getKey(), factors[i]
							* entry.getValue());
				}
			}
		}

		String[] result = new String[5];
		for (int i = 0; i < 5; i++) {
			// selecting the highest keys and retrieve them
			Float highestValue = 0F;
			String highestKey = "";
			for (Entry<String, Float> entry : combinedSearchResults.entrySet()) {
				if (entry.getValue() > highestValue) {
					highestValue = entry.getValue();
					highestKey = entry.getKey();
				}
			}
			combinedSearchResults.remove(highestKey);

			// TODO remove System.out.println:
			System.out.println(i + 1 + ": " + highestKey + ": " + highestValue);

			result[i] = highestKey;
		}
		return result;
	}

	@Override
	public int getCorpusId() {
		// TODO implement
		return 0;
	}

	@Override
	public void setCorpusId(int corpusId) {
		// TODO implement
	}

}
