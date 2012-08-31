package de.typology.lexerParser;

import static de.typology.lexerParser.EnronToken.ASTERISK;
import static de.typology.lexerParser.EnronToken.AT;
import static de.typology.lexerParser.EnronToken.BCC;
import static de.typology.lexerParser.EnronToken.BRACES;
import static de.typology.lexerParser.EnronToken.CC;
import static de.typology.lexerParser.EnronToken.CLOSEDBRACES;
import static de.typology.lexerParser.EnronToken.COLON;
import static de.typology.lexerParser.EnronToken.COMMA;
import static de.typology.lexerParser.EnronToken.CONTENTTRANSFERENCODING;
import static de.typology.lexerParser.EnronToken.CONTENTTYPE;
import static de.typology.lexerParser.EnronToken.DATE;
import static de.typology.lexerParser.EnronToken.EOF;
import static de.typology.lexerParser.EnronToken.EQUALITYSIGN;
import static de.typology.lexerParser.EnronToken.EXCLAMATIONMARK;
import static de.typology.lexerParser.EnronToken.FROM;
import static de.typology.lexerParser.EnronToken.FULLSTOP;
import static de.typology.lexerParser.EnronToken.HEADER;
import static de.typology.lexerParser.EnronToken.HYPHEN;
import static de.typology.lexerParser.EnronToken.LINESEPARATOR;
import static de.typology.lexerParser.EnronToken.MESSAGEID;
import static de.typology.lexerParser.EnronToken.MIMEVERSION;
import static de.typology.lexerParser.EnronToken.OTHER;
import static de.typology.lexerParser.EnronToken.QUESTIONMARK;
import static de.typology.lexerParser.EnronToken.QUOTATIONMARK;
import static de.typology.lexerParser.EnronToken.SLASH;
import static de.typology.lexerParser.EnronToken.STRING;
import static de.typology.lexerParser.EnronToken.SUBJECT;
import static de.typology.lexerParser.EnronToken.TO;
import static de.typology.lexerParser.EnronToken.VERTICALBAR;
import static de.typology.lexerParser.EnronToken.WS;
import static de.typology.lexerParser.EnronToken.XBCC;
import static de.typology.lexerParser.EnronToken.XCC;
import static de.typology.lexerParser.EnronToken.XFILENAME;
import static de.typology.lexerParser.EnronToken.XFOLDER;
import static de.typology.lexerParser.EnronToken.XFROM;
import static de.typology.lexerParser.EnronToken.XORIGIN;
import static de.typology.lexerParser.EnronToken.XTO;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Martin Koerner
 * 
 *         derived from
 *         http://101companies.org/index.php/101implementation:javaLexer
 * 
 */
public class EnronRecognizer implements Iterator<EnronToken> {

	private EnronToken token = null; // last token recognized
	private boolean eof = false; // reached end of file
	private Reader reader = null; // input stream
	private int lookahead = 0; // lookahead, if any
	private int[] buffer = new int[10000]; // lexeme buffer
	private int index = 0; // length of lexeme

	// Keywords to token mapping
	private static Map<String, EnronToken> keywords;

	static {
		keywords = new HashMap<String, EnronToken>();

		keywords.put("Message-ID", MESSAGEID);
		keywords.put("Date", DATE);
		keywords.put("From", FROM);
		keywords.put("To", TO);
		keywords.put("cc", CC);
		keywords.put("bcc", BCC);
		keywords.put("Cc", CC);
		keywords.put("Bcc", BCC);
		keywords.put("Subject", SUBJECT);
		keywords.put("Mime-Version", MIMEVERSION);
		keywords.put("Content-Type", CONTENTTYPE);
		keywords.put("Content-Transfer-Encoding", CONTENTTRANSFERENCODING);
		keywords.put("X-From", XFROM);
		keywords.put("X-To", XTO);
		keywords.put("X-cc", XCC);
		keywords.put("X-bcc", XBCC);
		keywords.put("X-Folder", XFOLDER);
		keywords.put("X-Origin", XORIGIN);
		keywords.put("X-FileName", XFILENAME);
	}

	public EnronRecognizer(File f) {
		try {
			this.reader = new BufferedReader(new FileReader(f));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
		if (this.lookahead == '\'') {
			this.read();
			this.token = QUOTATIONMARK;
			return;
		}
		// Recognize vertical bar
		if (this.lookahead == '|') {
			this.read();
			this.token = VERTICALBAR;
			return;
		}

		// Recognize slash
		if (this.lookahead == '/') {
			this.read();
			this.token = SLASH;
			return;
		}
		// Recognize colon
		if (this.lookahead == ':') {
			this.read();
			this.token = COLON;
			return;
		}
		// Recognize asterisk
		if (this.lookahead == '*') {
			this.read();
			this.token = ASTERISK;
			return;
		}
		// Recognize equality sign
		if (this.lookahead == '=') {
			this.read();
			this.token = EQUALITYSIGN;
			return;
		}

		// recognize quotation mark
		if (this.lookahead == 39) {// 39='
			this.read();
			this.token = QUOTATIONMARK;
			return;
		}

		// recognize @
		if (this.lookahead == '@') {
			this.read();
			this.token = AT;
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
			} while (!Character.isWhitespace(this.lookahead)
					&& Character.isLetterOrDigit(this.lookahead)
					|| this.lookahead == '-');
			// - and : to recognize header
			if (this.lookahead == ':') {
				if (keywords.containsKey(this.getLexeme())) {
					this.token = HEADER;
					return;
				}
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
	public EnronToken next() {
		if (this.hasNext()) {
			EnronToken result = this.token;
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
			EnronToken t = this.next();
			System.out.println(t + " : " + this.getLexeme());
		}
	}

}
