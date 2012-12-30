package de.typology.interfaces;

/**
 * Implementing this interface allows an object to predict the next word after a
 * series of four words.
 * 
 * @author Martin Koerner
 * 
 */
public interface Predictable {

	/**
	 * 
	 * @param fourGram
	 * @return result term
	 */
	public String[] predict(String[] fourGram, String prefix);

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
