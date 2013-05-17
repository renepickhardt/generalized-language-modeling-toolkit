package de.typology.parser;

import static de.typology.parser.Token.CLOSEDP;
import static de.typology.parser.Token.CLOSEDTEXT;
import static de.typology.parser.Token.CLOSEDTITLE;
import static de.typology.parser.Token.LESSTHAN;
import static de.typology.parser.Token.OTHER;
import static de.typology.parser.Token.P;
import static de.typology.parser.Token.TEXT;
import static de.typology.parser.Token.TITLE;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import de.typology.utils.IOHelper;

/**
 * @author Martin Koerner
 * 
 *         derived from
 *         http://101companies.org/index.php/101implementation:javaLexer
 * 
 */
public class ReutersRecognizer extends Tokenizer {

	// Keywords to token mapping
	private static Map<String, Token> keywords;

	static {
		keywords = new HashMap<String, Token>();
		keywords.put("p", P);
		keywords.put("title", TITLE);
		keywords.put("text", TEXT);

		keywords.put("/p", CLOSEDP);
		keywords.put("/title", CLOSEDTITLE);
		keywords.put("/text", CLOSEDTEXT);
	}

	public ReutersRecognizer(File f) {
		this.reader = IOHelper.openReadFile(f.getAbsolutePath());
	}

	// Recognize a token
	@Override
	public boolean lex() {
		super.lex();
		// Recognize <???>
		if (this.token == LESSTHAN) {
			do {
				this.read();

			} while (this.lookahead != '>');
			this.read();
			String label = (String) this.getLexeme().subSequence(1,
					this.getLexeme().length() - 1);
			if (keywords.containsKey(label)) {
				this.token = keywords.get(label);
			} else {
				this.token = OTHER;
			}
			return true;
		}
		super.lexGeneral();
		return true;
	}
}
