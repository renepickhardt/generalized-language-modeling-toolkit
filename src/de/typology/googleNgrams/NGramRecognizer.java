package de.typology.googleNgrams;

import static de.typology.googleNgrams.NGramToken.COLON;
import static de.typology.googleNgrams.NGramToken.COMMA;
import static de.typology.googleNgrams.NGramToken.EOF;
import static de.typology.googleNgrams.NGramToken.EXCLAMATIONMARK;
import static de.typology.googleNgrams.NGramToken.FULLSTOP;
import static de.typology.googleNgrams.NGramToken.HYPHEN;
import static de.typology.googleNgrams.NGramToken.LINESEPARATOR;
import static de.typology.googleNgrams.NGramToken.OTHER;
import static de.typology.googleNgrams.NGramToken.QUESTIONMARK;
import static de.typology.googleNgrams.NGramToken.QUOTATIONMARK;
import static de.typology.googleNgrams.NGramToken.SEMICOLON;
import static de.typology.googleNgrams.NGramToken.STRING;
import static de.typology.googleNgrams.NGramToken.WS;

import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;

import de.typology.lexerParser.RecognitionException;
import de.typology.utils.IOHelper;

/**
 * @author Martin Koerner
 * 
 *         derived from
 *         http://101companies.org/index.php/101implementation:javaLexer
 * 
 */
public class NGramRecognizer implements Iterator<NGramToken> {

	private NGramToken token = null; // last token recognized
	private boolean eof = false; // reached end of file
	private Reader reader = null; // input stream
	private int lookahead = 0; // lookahead, if any
	private int[] buffer = new int[10000]; // lexeme buffer
	private int index = 0; // length of lexeme

	public NGramRecognizer(String input) {
		this.reader = IOHelper.openReadFile(input);
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
		if (Character.isWhitespace(this.lookahead)) {
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
		// Recognize semicolon
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
		if (this.lookahead == '!' || this.lookahead == '¡') {
			this.read();
			this.token = EXCLAMATIONMARK;
			return;
		}

		// Recognize question mark
		if (this.lookahead == '?' || this.lookahead == '¿') {
			this.read();
			this.token = QUESTIONMARK;
			return;
		}
		// recognize quotation mark
		if (this.lookahead == '\'') {
			this.read();
			this.token = QUOTATIONMARK;
			return;
		}

		// Recognize end of file
		if (this.lookahead == -1) {
			this.eof = true;
			this.token = EOF;
			return;
		}

		// Recognize String
		if (Character.isLetterOrDigit(this.lookahead)) {
			do {
				this.read();
			} while (Character.isLetterOrDigit(this.lookahead));
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
	public NGramToken next() {
		if (this.hasNext()) {
			NGramToken result = this.token;
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
			NGramToken t = this.next();
			System.out.println(t + " : " + this.getLexeme());
		}
	}
}
