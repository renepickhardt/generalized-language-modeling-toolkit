package de.typology.parser;

import static de.typology.parser.Token.AND;
import static de.typology.parser.Token.CLOSEDP;
import static de.typology.parser.Token.CLOSEDROUNDBRACKET;
import static de.typology.parser.Token.CLOSEDTEXT;
import static de.typology.parser.Token.CLOSEDTITLE;
import static de.typology.parser.Token.COLON;
import static de.typology.parser.Token.COMMA;
import static de.typology.parser.Token.EOF;
import static de.typology.parser.Token.EXCLAMATIONMARK;
import static de.typology.parser.Token.FULLSTOP;
import static de.typology.parser.Token.HYPHEN;
import static de.typology.parser.Token.LINESEPARATOR;
import static de.typology.parser.Token.OTHER;
import static de.typology.parser.Token.P;
import static de.typology.parser.Token.QUESTIONMARK;
import static de.typology.parser.Token.QUOTATIONMARK;
import static de.typology.parser.Token.ROUNDBRACKET;
import static de.typology.parser.Token.SEMICOLON;
import static de.typology.parser.Token.STRING;
import static de.typology.parser.Token.TEXT;
import static de.typology.parser.Token.TITLE;
import static de.typology.parser.Token.WS;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import de.typology.utils.IOHelper;

/**
 * @author Martin Koerner
 * 
 *         derived from
 *         http://101companies.org/index.php/101implementation:javaLexer
 * 
 */
public class ReutersRecognizer implements Iterator<Token> {

	private Token token = null; // last token recognized
	private boolean eof = false; // reached end of file
	private Reader reader = null; // input stream
	private int lookahead = 0; // lookahead, if any
	private int[] buffer = new int[10000]; // lexeme buffer
	private int index = 0; // length of lexeme

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
		if (this.lookahead == 10) {
			this.token = LINESEPARATOR;
			this.read();
			return;
		}
		// Recognize whitespace
		if (Character.isWhitespace(this.lookahead)) {
			do {
				this.read();
			} while (Character.isWhitespace(this.lookahead));// removes multiple
																// spaces
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

		// recognize quotation mark
		if (this.lookahead == 39) {// 39='
			this.read();
			this.token = QUOTATIONMARK;
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
			this.token = ROUNDBRACKET;
			return;
		}

		// Recognize braces close
		if (this.lookahead == ')') {
			this.read();
			this.token = CLOSEDROUNDBRACKET;
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
	public Token next() {
		if (this.hasNext()) {
			Token result = this.token;
			this.token = null;
			return result;
		} else {
			throw new IllegalStateException();
		}
	}

	public void close() {
		try {
			this.reader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	// Stress test: lex until end-of-file
	public void lexall() {
		while (this.hasNext()) {
			Token t = this.next();
			System.out.println(t + " : " + this.getLexeme());
		}
	}
}
