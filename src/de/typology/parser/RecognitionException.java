package de.typology.parser;

/**
 * @author Martin Koerner
 * 
 *         derived from
 *         http://101companies.org/index.php/101implementation:javaLexer
 * 
 */
public class RecognitionException extends RuntimeException {
	private static final long serialVersionUID = 6584696724594981863L;

	public RecognitionException() {
	}

	public RecognitionException(String s) {
		super(s);
	}

	public RecognitionException(Throwable t) {
		super(t);
	}
}
