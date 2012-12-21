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
	 * @param nGramReader
	 * @return runtime in ms
	 */
	public double train(String path);

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
