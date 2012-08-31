package de.typology.lexerParser;

import static de.typology.lexerParser.EnronToken.AT;
import static de.typology.lexerParser.EnronToken.BRACES;
import static de.typology.lexerParser.EnronToken.CLOSEDBRACES;
import static de.typology.lexerParser.EnronToken.COMMA;
import static de.typology.lexerParser.EnronToken.FULLSTOP;
import static de.typology.lexerParser.EnronToken.HEADER;
import static de.typology.lexerParser.EnronToken.HYPHEN;
import static de.typology.lexerParser.EnronToken.LINESEPARATOR;
import static de.typology.lexerParser.EnronToken.SEMICOLON;
import static de.typology.lexerParser.EnronToken.STRING;
import static de.typology.lexerParser.EnronToken.WS;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;

import de.typology.utils.Config;

/**
 * @author Martin Koerner
 * 
 *         derived from
 *         http://101companies.org/index.php/101implementation:javaLexer
 * 
 */
public class EnronParser {
	private EnronRecognizer recognizer;
	private String lexeme = new String();
	private int bracketCount;
	boolean lastLineWasAHeader;
	boolean isString;
	private EnronToken current;
	private EnronToken previous;
	private Writer writer;
	private ArrayList<File> fileList;

	public EnronParser(ArrayList<File> fileList) throws FileNotFoundException {
		this.fileList = fileList;
		this.writer = new OutputStreamWriter(new FileOutputStream(
				Config.get().parsedEnronOutputPath));

	}

	public void parse() throws IOException {
		for (File f : this.fileList) {
			this.recognizer = new EnronRecognizer(f);
			// write(f.toString());
			// write("\n");
			this.lastLineWasAHeader = false;
			while (this.recognizer.hasNext()) {
				this.read();

				// Remove header
				if (this.current == HEADER) {
					while (this.current != LINESEPARATOR
							&& this.recognizer.hasNext()) {
						this.skip();
					}
					if (this.current == LINESEPARATOR) {
						this.skip();
					}
					this.lastLineWasAHeader = true;
				}

				// Remove word wraps inside a header
				if (this.lastLineWasAHeader == true
						&& this.previous == LINESEPARATOR && this.current == WS) {
					while (this.current != LINESEPARATOR
							&& this.recognizer.hasNext()) {
						this.skip();
					}
					if (this.current == LINESEPARATOR) {
						this.skip();
					}
				}

				// Remove lines that start with hyphen
				if (this.current == HYPHEN && this.previous == LINESEPARATOR) {
					while (this.current != LINESEPARATOR
							&& this.recognizer.hasNext()) {
						this.current = this.recognizer.next();
					}
				}

				// Remove upper case strings
				if (this.current == STRING) {
					if (!this.lexeme.matches(".+[A-Z].*")) {
						this.write(this.lexeme);
					}
				}
				if (this.current == LINESEPARATOR) {
					this.write(" ");
					this.lastLineWasAHeader = false;
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
				if (this.current == HYPHEN) {
					this.write("-");
				}
				if (this.current == AT) {
					this.write("@");
				}
				if (this.current == WS) {
					this.write(" ");
				}
				if (this.current == BRACES) {
					while (this.recognizer.hasNext()
							&& this.current != CLOSEDBRACES) {
						this.current = this.recognizer.next();
					}
				}
			}
			this.write("\n");// new line after file
		}
		this.writer.close();
	}

	public void read() throws IOException {
		if (this.recognizer.hasNext()) {
			this.previous = this.current;
			this.current = this.recognizer.next();
			this.lexeme = this.recognizer.getLexeme();
		} else {
			throw new IllegalStateException();
		}
	}

	public void skip() {
		if (this.recognizer.hasNext()) {
			this.previous = this.current;
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
