package de.typology.parser;

import static de.typology.parser.Token.BODY;
import static de.typology.parser.Token.CLOSEDBODY;
import static de.typology.parser.Token.CLOSEDSEG;
import static de.typology.parser.Token.CLOSEDTUV;
import static de.typology.parser.Token.OTHER;
import static de.typology.parser.Token.SEG;
import static de.typology.parser.Token.TUV;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Martin Koerner
 * 
 *         derived from
 *         http://101companies.org/index.php/101implementation:javaLexer
 * 
 */
public class AcquisTokenizer extends Tokenizer {
	// Keywords to token mapping
	private static Map<String, Token> keywords;
	private static Map<String, String> tuvs;

	static {
		tuvs = new HashMap<String, String>();
		tuvs.put("en", "tuv lang=\"EN-GB\"");
		tuvs.put("de", "tuv lang=\"DE-DE\"");
		tuvs.put("es", "tuv lang=\"ES-ES\"");
		tuvs.put("fr", "tuv lang=\"FR-FR\"");
		tuvs.put("it", "tuv lang=\"IT-IT\"");
		// add new languages here
	}

	public AcquisTokenizer(File f, String acquisLanguage) {
		Reader r;
		try {
			r = new InputStreamReader(new FileInputStream(f.getAbsolutePath()),
					"UnicodeLittle");
			this.reader = new BufferedReader(r);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		keywords = new HashMap<String, Token>();
		keywords.put("body", BODY);
		keywords.put("seg", SEG);
		keywords.put("/body", CLOSEDBODY);
		keywords.put("/tuv", CLOSEDTUV);
		keywords.put("/seg", CLOSEDSEG);
		// set language specific header
		keywords.put(tuvs.get(acquisLanguage), TUV);
	}

	// Recognize a token
	@Override
	public boolean lex() {
		super.lex();
		if (this.token == Token.LESSTHAN) {
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
