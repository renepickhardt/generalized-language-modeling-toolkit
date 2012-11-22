package de.typology.googleNGrams;

import static de.typology.googleNGrams.NGramToken.COLON;
import static de.typology.googleNGrams.NGramToken.COMMA;
import static de.typology.googleNGrams.NGramToken.EXCLAMATIONMARK;
import static de.typology.googleNGrams.NGramToken.FULLSTOP;
import static de.typology.googleNGrams.NGramToken.HYPHEN;
import static de.typology.googleNGrams.NGramToken.LINESEPARATOR;
import static de.typology.googleNGrams.NGramToken.QUESTIONMARK;
import static de.typology.googleNGrams.NGramToken.QUOTATIONMARK;
import static de.typology.googleNGrams.NGramToken.SEMICOLON;
import static de.typology.googleNGrams.NGramToken.STRING;
import static de.typology.googleNGrams.NGramToken.WS;

import java.io.IOException;
import java.io.Writer;

import de.typology.utils.Config;
import de.typology.utils.IOHelper;

/**
 * Given a NGramRecognizer, this parser only prints declared parts of the ngram.
 * <p>
 * derived from http://101companies.org/index.php/101implementation:javaLexer
 * 
 * @author Martin Koerner
 */
public class NGramParser {
	private NGramRecognizer recognizer;
	private String lexeme = new String();
	boolean lastLineWasAHeader;
	boolean isString;
	private NGramToken current;
	private Writer writer;

	public NGramParser(NGramRecognizer recognizer) {
		this.recognizer = recognizer;
		this.writer = IOHelper.openWriteFile(
				Config.get().parsedGoogleNGramsOutputPath, 32 * 1024 * 1024);
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
