package de.typology.interfaces;

/**
 * Implementing this interface allows an object to build a prediction model
 * using five grams.
 * 
 * @author Martin Koerner
 * 
 */
public interface Trainable {

	/**
	 * 
	 * @param pathToFiveGrams
	 */
	public void train(String pathToFiveGrams);

	/**
	 * 
	 * @return corpusId
	 */
	public int getCorpusId();

	/**
	 * 
	 * @param corpusId
	 */
	public void setCorpusId(int corpusId);
}
