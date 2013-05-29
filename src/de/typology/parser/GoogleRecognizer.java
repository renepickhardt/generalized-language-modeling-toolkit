package de.typology.parser;

import de.typology.utils.IOHelper;

/**
 * derived from http://101companies.org/index.php/101implementation:javaLexer
 * 
 * @author Martin Koerner
 */
public class GoogleRecognizer extends Tokenizer {

	public GoogleRecognizer(String input) {
		this.reader = IOHelper.openReadFile(input);
	}

	// Recognize a token
	@Override
	public boolean lex() {

		super.lex();
		super.lexGeneral();
		return true;
	}

}
