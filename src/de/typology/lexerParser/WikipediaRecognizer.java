package de.typology.lexerParser;

import static de.typology.lexerParser.WikipediaToken.CLOSEDPAGE;
import static de.typology.lexerParser.WikipediaToken.CLOSEDTEXT;
import static de.typology.lexerParser.WikipediaToken.CLOSEDTITLE;
import static de.typology.lexerParser.WikipediaToken.COMMA;
import static de.typology.lexerParser.WikipediaToken.EM;
import static de.typology.lexerParser.WikipediaToken.EOF;
import static de.typology.lexerParser.WikipediaToken.FULLSTOP;
import static de.typology.lexerParser.WikipediaToken.INFOBOX;
import static de.typology.lexerParser.WikipediaToken.LABELEDLINK;
import static de.typology.lexerParser.WikipediaToken.LINESEPERATOR;
import static de.typology.lexerParser.WikipediaToken.LINK;
import static de.typology.lexerParser.WikipediaToken.OTHER;
import static de.typology.lexerParser.WikipediaToken.PAGE;
import static de.typology.lexerParser.WikipediaToken.QM;
import static de.typology.lexerParser.WikipediaToken.STRING;
import static de.typology.lexerParser.WikipediaToken.TEXT;
import static de.typology.lexerParser.WikipediaToken.TITLE;
import static de.typology.lexerParser.WikipediaToken.URI;
import static de.typology.lexerParser.WikipediaToken.WS;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.itadaki.bzip2.BZip2InputStream;

/**
 * @author Martin Koerner
 * 
 *         derived from
 *         http://101companies.org/index.php/101implementation:javaLexer
 * 
 */
public class WikipediaRecognizer implements Iterator<WikipediaToken> {

	private WikipediaToken token = null; // last token recognized
	private boolean eof = false; // reached end of file
	private Reader reader = null; // input stream
	private int lookahead = 0; // lookahead, if any
	private int[] buffer = new int[10000]; // lexeme buffer
	private int index = 0; // length of lexeme

	// Keywords to token mapping
	private static Map<String, WikipediaToken> keywords;

	static {
		int[] a = { 1, 2, 3 };
		HashMap h = new HashMap();
		keywords = new HashMap<String, WikipediaToken>();
		keywords.put("page", PAGE);
		keywords.put("title", TITLE);
		keywords.put("text xml:space=\"preserve\"", TEXT);

		keywords.put("/page", CLOSEDPAGE);
		keywords.put("/title", CLOSEDTITLE);
		keywords.put("/text", CLOSEDTEXT);
	}

	public WikipediaRecognizer(String s) throws FileNotFoundException {
		InputStream input = new FileInputStream(new File(s));
		BZip2InputStream cb = new BZip2InputStream(input, false);
		this.reader = new BufferedReader(new InputStreamReader(cb));
		// this.reader = new BufferedReader(new FileReader(new File(s)));
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
			this.token = LINESEPERATOR;
			do {
				this.read();
			} while (Character.isWhitespace(this.lookahead));
			// removes multiple spaces
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

		// Recognize exclamation mark
		if (this.lookahead == '!') {
			this.read();
			this.token = EM;
			return;
		}

		// Recognize question mark
		if (this.lookahead == '?') {
			this.read();
			this.token = QM;
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

		// Recognize link
		if (this.lookahead == '[') {
			this.read();
			if (this.lookahead == '[') {
				this.read();
				this.token = LINK;
				while (this.lookahead != ']') {
					if (this.lookahead == '|') {
						this.token = LABELEDLINK;
					}
					this.read();
				}
				this.read();
				if (this.lookahead == ']') {
					this.read();
				} else {
					this.token = OTHER;
				}
				return;

			}
		}

		// Recognize infobox
		if (this.lookahead == '{') {
			this.read();
			if (this.lookahead == '{') {
				this.read();
				this.token = INFOBOX;
				while (this.lookahead != 10) {
					this.read();
				}
			} else {
				this.token = OTHER;
			}
			return;
		}

		// Recognize String
		if (Character.isLetterOrDigit(this.lookahead)) {

			// Recognize special String: URI
			if (this.lookahead == 'h') {
				this.read();
				if (this.lookahead == 't') {
					this.read();
					if (this.lookahead == 't') {
						this.read();
						if (this.lookahead == 'p') {
							this.read();
							if (this.lookahead == ':') {
								while (!Character.isWhitespace(this.lookahead)
										&& this.lookahead != '<') {
									this.read();
								}
								this.token = URI;
								return;
							}
						}
					}
				}
			}

			while (!Character.isWhitespace(this.lookahead)
					&& Character.isLetterOrDigit(this.lookahead)) {
				this.read();
			}
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
	public WikipediaToken next() {
		if (this.hasNext()) {
			WikipediaToken result = this.token;
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
			WikipediaToken t = this.next();
			System.out.println(t + " : " + this.getLexeme());
		}
	}
}
