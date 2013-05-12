package de.typology.parser;

import static de.typology.parser.Token.CLOSEDP;
import static de.typology.parser.Token.CLOSEDROUNDBRACKET;
import static de.typology.parser.Token.CLOSEDTEXT;
import static de.typology.parser.Token.COLON;
import static de.typology.parser.Token.COMMA;
import static de.typology.parser.Token.EXCLAMATIONMARK;
import static de.typology.parser.Token.FULLSTOP;
import static de.typology.parser.Token.HYPHEN;
import static de.typology.parser.Token.P;
import static de.typology.parser.Token.QUESTIONMARK;
import static de.typology.parser.Token.QUOTATIONMARK;
import static de.typology.parser.Token.ROUNDBRACKET;
import static de.typology.parser.Token.SEMICOLON;
import static de.typology.parser.Token.STRING;
import static de.typology.parser.Token.TEXT;
import static de.typology.parser.Token.WS;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

import de.typology.utils.IOHelper;

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
	boolean lastLineWasAHeader;
	boolean isString;
	private Token current;
	// private ReutersToken previous;
	private Writer writer;
	private ArrayList<File> fileList;

	public ReutersParser(ArrayList<File> fileList, String output)
			throws FileNotFoundException {
		this.fileList = fileList;
		this.writer = IOHelper.openWriteFile(output, 32 * 1024 * 1024);

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
								if (this.current == SEMICOLON) {
									this.write(this.lexeme);
								}
								if (this.current == COLON) {
									this.write(": ");
								}
								if (this.current == QUOTATIONMARK) {
									this.write("'");
								}
								if (this.current == HYPHEN) {
									this.write("-");
								}
								if (this.current == QUESTIONMARK) {
									this.write("? ");
								}
								if (this.current == EXCLAMATIONMARK) {
									this.write("! ");
								}
								if (this.current == WS) {
									this.write(" ");
								}
								if (this.current == ROUNDBRACKET) {
									while (this.recognizer.hasNext()
											&& this.current != CLOSEDROUNDBRACKET
											&& this.current != CLOSEDP) {
										this.skip();
									}
								}

							}
							this.write(" ");// space after article
						}
					}
				}
			}
			this.write("\n");// new line after page
			this.recognizer.close();
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

	public void skip() {
		if (this.recognizer.hasNext()) {
			// this.previous = this.current;
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
