package de.typology.googleNgrams;

import static de.typology.googleNgrams.NGramToken.COLON;
import static de.typology.googleNgrams.NGramToken.COMMA;
import static de.typology.googleNgrams.NGramToken.EXCLAMATIONMARK;
import static de.typology.googleNgrams.NGramToken.FULLSTOP;
import static de.typology.googleNgrams.NGramToken.HYPHEN;
import static de.typology.googleNgrams.NGramToken.LINESEPARATOR;
import static de.typology.googleNgrams.NGramToken.QUESTIONMARK;
import static de.typology.googleNgrams.NGramToken.QUOTATIONMARK;
import static de.typology.googleNgrams.NGramToken.SEMICOLON;
import static de.typology.googleNgrams.NGramToken.STRING;
import static de.typology.googleNgrams.NGramToken.WS;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import de.typology.utils.Config;

/**
 * @author Martin Koerner
 * 
 *         derived from
 *         http://101companies.org/index.php/101implementation:javaLexer
 * 
 */
public class NGramParser {
	private NGramRecognizer recognizer;
	private String lexeme = new String();
	boolean lastLineWasAHeader;
	boolean isString;
	private NGramToken current;
	private Writer writer;

	public NGramParser(NGramRecognizer recognizer) throws FileNotFoundException {
		this.recognizer = recognizer;
		this.writer = new OutputStreamWriter(new FileOutputStream(
				Config.get().parsedGoogleNGramsOutputPath));
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
				this.write("\t");
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
