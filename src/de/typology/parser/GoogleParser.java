package de.typology.parser;

import static de.typology.parser.GoogleToken.COLON;
import static de.typology.parser.GoogleToken.COMMA;
import static de.typology.parser.GoogleToken.EXCLAMATIONMARK;
import static de.typology.parser.GoogleToken.FULLSTOP;
import static de.typology.parser.GoogleToken.HYPHEN;
import static de.typology.parser.GoogleToken.LINESEPARATOR;
import static de.typology.parser.GoogleToken.QUESTIONMARK;
import static de.typology.parser.GoogleToken.QUOTATIONMARK;
import static de.typology.parser.GoogleToken.SEMICOLON;
import static de.typology.parser.GoogleToken.STRING;
import static de.typology.parser.GoogleToken.WS;

import java.io.IOException;
import java.io.Writer;

import de.typology.utils.IOHelper;

/**
 * Given a NGramRecognizer, this parser only prints declared parts of the ngram.
 * <p>
 * derived from http://101companies.org/index.php/101implementation:javaLexer
 * 
 * @author Martin Koerner
 */
public class GoogleParser {
	private GoogleRecognizer recognizer;
	private String lexeme = new String();
	boolean lastLineWasAHeader;
	boolean isString;
	private GoogleToken current;
	private Writer writer;

	public GoogleParser(GoogleRecognizer recognizer, String parsedGoogleOutputPath) {
		this.recognizer = recognizer;
		this.writer = IOHelper.openWriteFile(parsedGoogleOutputPath,
				32 * 1024 * 1024);
	}

	public void parse() throws IOException {
		this.reset();
		while (this.recognizer.hasNext()) {
			this.read();
			if (this.current == STRING) {
				this.write(this.lexeme);
			}
			if (this.current == FULLSTOP) {
				this.write(this.lexeme);
			}
			if (this.current == COMMA) {
				this.write(this.lexeme);
			}
			if (this.current == SEMICOLON) {
				this.write(this.lexeme);
			}
			if (this.current == COLON) {
				this.write(this.lexeme);
			}
			if (this.current == HYPHEN) {
				this.write("-");
			}
			if (this.current == QUESTIONMARK) {
				this.write(this.lexeme);
			}
			if (this.current == EXCLAMATIONMARK) {
				this.write(this.lexeme);
			}
			if (this.current == QUOTATIONMARK) {
				this.write("'");
			}
			if (this.current == WS) {
				this.write(" ");
			}
			if (this.current == LINESEPARATOR) {
				this.write("\n");
			}
		}
		this.writer.close();
	}

	public void read() throws IOException {
		if (this.recognizer.hasNext()) {
			// this.previous = this.current;
			this.current = this.recognizer.next();
			this.lexeme = this.recognizer.getLexeme();
		} else {
			throw new IllegalStateException();
		}
	}

	public void reset() {
		this.lexeme = "";
		this.current = null;
	}

	public void skip() {
		if (this.recognizer.hasNext()) {
			this.current = this.recognizer.next();
		} else {
			throw new IllegalStateException();
		}
	}

	public void write(String s) {
		try {
			this.writer.write(s);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
