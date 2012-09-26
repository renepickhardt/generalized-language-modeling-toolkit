package de.typology.lexerParser;

import static de.typology.lexerParser.DGTTMToken.AND;
import static de.typology.lexerParser.DGTTMToken.BODY;
import static de.typology.lexerParser.DGTTMToken.BRACES;
import static de.typology.lexerParser.DGTTMToken.CLOSEDBODY;
import static de.typology.lexerParser.DGTTMToken.CLOSEDBRACES;
import static de.typology.lexerParser.DGTTMToken.CLOSEDSEG;
import static de.typology.lexerParser.DGTTMToken.CLOSEDTUV;
import static de.typology.lexerParser.DGTTMToken.COLON;
import static de.typology.lexerParser.DGTTMToken.COMMA;
import static de.typology.lexerParser.DGTTMToken.EOF;
import static de.typology.lexerParser.DGTTMToken.EXCLAMATIONMARK;
import static de.typology.lexerParser.DGTTMToken.FULLSTOP;
import static de.typology.lexerParser.DGTTMToken.HYPHEN;
import static de.typology.lexerParser.DGTTMToken.LINESEPARATOR;
import static de.typology.lexerParser.DGTTMToken.OTHER;
import static de.typology.lexerParser.DGTTMToken.QUESTIONMARK;
import static de.typology.lexerParser.DGTTMToken.QUOTATIONMARK;
import static de.typology.lexerParser.DGTTMToken.SEG;
import static de.typology.lexerParser.DGTTMToken.SEMICOLON;
import static de.typology.lexerParser.DGTTMToken.STRING;
import static de.typology.lexerParser.DGTTMToken.TUV;
import static de.typology.lexerParser.DGTTMToken.WS;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import de.typology.utils.Config;

/**
 * @author Martin Koerner
 * 
 *         derived from
 *         http://101companies.org/index.php/101implementation:javaLexer
 * 
 */
public class DGTTMRecognizer implements Iterator<DGTTMToken> {

	private DGTTMToken token = null; // last token recognized
	private boolean eof = false; // reached end of file
	private Reader reader = null; // input stream
	private int lookahead = 0; // lookahead, if any
	private int[] buffer = new int[10000]; // lexeme buffer
	private int index = 0; // length of lexeme

	// Keywords to token mapping
	private static Map<String, DGTTMToken> keywords;
	private static Map<String, String> tuvs;
	static {
		keywords = new HashMap<String, DGTTMToken>();
		keywords.put("body", BODY);
		keywords.put("seg", SEG);
		keywords.put("/body", CLOSEDBODY);
		keywords.put("/tuv", CLOSEDTUV);
		keywords.put("/seg", CLOSEDSEG);
	}
	static {
		tuvs = new HashMap<String, String>();
		tuvs.put("EN", "tuv lang=\"EN-GB\"");
		tuvs.put("DE", "tuv lang=\"DE-DE\"");
		tuvs.put("ES", "tuv lang=\"ES-ES\"");
		tuvs.put("FR", "tuv lang=\"FR-FR\"");
		tuvs.put("IT", "tuv lang=\"IT-IT\"");
		// add new languages here
	}

	public DGTTMRecognizer(File f) throws UnsupportedEncodingException,
			FileNotFoundException {
		Reader r = new InputStreamReader(new FileInputStream(
				f.getAbsolutePath()), "UnicodeLittle");
		this.reader = new BufferedReader(r);
		// set language specific header
		keywords.put(tuvs.get(Config.get().DGTTMLanguage), TUV);
	}

	// Extract lexeme from buffer
	public String getLexeme() {
		return new String(this.buffer, 0, this.index);
	}

	// Reset state to begin new token
	private void reset() throws IOException {
		if (this.eof) {
			throw new IllegalStateException();
		}
		this.index = 0;
		this.token = null;
		if (this.lookahead == 0) {
			this.read();
		}
	}

	// Read one more char.
	// Add previous char, if any, to the buffer.
	//
	private void read() throws IOException {
		if (this.eof) {
			throw new IllegalStateException();
		}
		if (this.lookahead != 0) {
			this.buffer[this.index] = this.lookahead;
			this.index++;
			if (this.index == 10000) {
				this.index = 0;
				// reset buffer if token gets too big (very unlikely to happen)
			}
		}
		this.lookahead = this.reader.read();
	}

	// Recognize a token
	public void lex() throws IOException {
		this.reset();

		// Recognize newline
		if (this.lookahead == 13) {
			this.token = LINESEPARATOR;
			this.read();
			if (this.lookahead == 10) {
				this.token = LINESEPARATOR;
				this.read();
			}
			return;
		}
		// Recognize newline
		if (this.lookahead == 10) {
			this.token = LINESEPARATOR;
			this.read();
			return;
		}
		// Recognize whitespace
		if (Character.isWhitespace(this.lookahead) || this.lookahead == ' ') {
			this.read();
			this.token = WS;
			return;
		}

		// Recognize fullstop
		if (this.lookahead == '.') {
			this.read();
			this.token = FULLSTOP;
			return;
		}

		// Recognize comma
		if (this.lookahead == ',') {
			this.read();
			this.token = COMMA;
			return;
		}

		// Recognize semicolon
		if (this.lookahead == ';') {
			this.read();
			this.token = SEMICOLON;
			return;
		}
		// Recognize colon
		if (this.lookahead == ':') {
			this.read();
			this.token = COLON;
			return;
		}

		// Recognize hyphen
		if (this.lookahead == '-') {
			this.read();
			this.token = HYPHEN;
			return;
		}
		// Recognize dash (as hyphen)
		if (this.lookahead == '–') {
			this.read();
			this.token = HYPHEN;
			return;
		}

		// Recognize exclamation mark
		if (this.lookahead == '!') {
			this.read();
			this.token = EXCLAMATIONMARK;
			return;
		}

		// Recognize question mark
		if (this.lookahead == '?') {
			this.read();
			this.token = QUESTIONMARK;
			return;
		}
		// recognize quotation mark
		if (this.lookahead == 39) {// 39='
			this.read();
			this.token = QUOTATIONMARK;
			return;
		}

		// Recognize quotation mark
		if (this.lookahead == '&') {
			this.read();
			if (this.lookahead == 'q') {
				this.read();
				if (this.lookahead == 'u') {
					this.read();
					if (this.lookahead == 'o') {
						this.read();
						if (this.lookahead == 't') {
							this.read();
							if (this.lookahead == ';') {
								this.read();
								this.token = QUOTATIONMARK;
								return;
							}
						}
					}
				}
			}
			if (this.lookahead == 'a') {
				this.read();
				if (this.lookahead == 'm') {
					this.read();
					if (this.lookahead == 'p') {
						this.read();
						if (this.lookahead == ';') {
							this.read();
							this.token = AND;
							return;
						}
					}

				}
			}
			this.token = OTHER;
			return;

		}
		// Recognize end of file
		if (this.lookahead == -1) {
			this.eof = true;
			this.token = EOF;
			return;
		}

		// Recognize <???>
		if (this.lookahead == '<') {
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
			return;
		}

		// Recognize braces open
		if (this.lookahead == '(') {
			this.read();
			this.token = BRACES;
			return;
		}

		// Recognize braces close
		if (this.lookahead == ')') {
			this.read();
			this.token = CLOSEDBRACES;
			return;
		}

		// Recognize String
		if (Character.isLetterOrDigit(this.lookahead) && this.lookahead != '&') {
			do {
				this.read();
			} while (!Character.isWhitespace(this.lookahead)
					&& Character.isLetterOrDigit(this.lookahead));
			this.token = STRING;
			return;
		}

		// Recognize other
		if (!Character.isLetterOrDigit(this.lookahead)) {
			this.read();
			this.token = OTHER;
			return;
		}

		throw new RecognitionException("Recognizer giving up at "
				+ this.lookahead);
	}

	@Override
	public boolean hasNext() {
		if (this.token != null) {
			return true;
		}
		if (this.eof) {
			return false;
		}
		try {
			this.lex();
		} catch (IOException e) {
			throw new RecognitionException(e);
		}
		return true;
	}

	@Override
	public DGTTMToken next() {
		if (this.hasNext()) {
			DGTTMToken result = this.token;
			this.token = null;
			return result;
		} else {
			throw new IllegalStateException();
		}
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	// Stress test: lex until end-of-file
	public void lexall() {
		while (this.hasNext()) {
			DGTTMToken t = this.next();
			System.out.println(t + " : " + this.getLexeme());
		}
	}
}
