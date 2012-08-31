package de.typology.lexerParser;

import static de.typology.lexerParser.ReutersToken.BRACES;
import static de.typology.lexerParser.ReutersToken.CLOSEDBRACES;
import static de.typology.lexerParser.ReutersToken.CLOSEDP;
import static de.typology.lexerParser.ReutersToken.CLOSEDTEXT;
import static de.typology.lexerParser.ReutersToken.COMMA;
import static de.typology.lexerParser.ReutersToken.FULLSTOP;
import static de.typology.lexerParser.ReutersToken.HYPHEN;
import static de.typology.lexerParser.ReutersToken.P;
import static de.typology.lexerParser.ReutersToken.STRING;
import static de.typology.lexerParser.ReutersToken.TEXT;
import static de.typology.lexerParser.ReutersToken.WS;

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
public class ReutersParser {
	private ReutersRecognizer recognizer;
	private String lexeme = new String();
	private int bracketCount;
	boolean lastLineWasAHeader;
	boolean isString;
	private ReutersToken current;
	private ReutersToken previous;
	private Writer writer;
	private ArrayList<File> fileList;

	public ReutersParser(ArrayList<File> fileList) throws FileNotFoundException {
		this.fileList = fileList;
		this.writer = new OutputStreamWriter(new FileOutputStream(
				Config.get().parsedReutersOutputPath));

	}

	public void parse() throws IOException {
		for (File f : this.fileList) {
			this.recognizer = new ReutersRecognizer(f);
			// writer.write(f.toString());
			// writer.write("\n");
			while (this.recognizer.hasNext()) {
				this.read();
				if (this.current == TEXT) {
					while (this.recognizer.hasNext()
							&& this.current != CLOSEDTEXT) {
						// inside a textblock
						this.read();
						if (this.current == P) {
							while (this.recognizer.hasNext()
									&& this.current != CLOSEDP) {
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
								if (this.current == HYPHEN) {
									this.write("-");
								}

								if (this.current == WS) {
									this.write(" ");
								}
								if (this.current == BRACES) {
									while (this.recognizer.hasNext()
											&& this.current != CLOSEDBRACES
											&& this.current != CLOSEDP) {
										this.skip();
									}
								}

							}
							this.write("\n");// new line after arcticle
						}
					}
				}
			}
			this.write("\n");// new line after page
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
