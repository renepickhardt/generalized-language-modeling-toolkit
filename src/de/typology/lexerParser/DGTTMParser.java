package de.typology.lexerParser;

import static de.typology.lexerParser.DGTTMToken.BODY;
import static de.typology.lexerParser.DGTTMToken.BRACES;
import static de.typology.lexerParser.DGTTMToken.CLOSEDBODY;
import static de.typology.lexerParser.DGTTMToken.CLOSEDBRACES;
import static de.typology.lexerParser.DGTTMToken.CLOSEDTUV;
import static de.typology.lexerParser.DGTTMToken.COMMA;
import static de.typology.lexerParser.DGTTMToken.FULLSTOP;
import static de.typology.lexerParser.DGTTMToken.HYPHEN;
import static de.typology.lexerParser.DGTTMToken.SEMICOLON;
import static de.typology.lexerParser.DGTTMToken.STRING;
import static de.typology.lexerParser.DGTTMToken.TUV;
import static de.typology.lexerParser.DGTTMToken.WS;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;

/**
 * @author Martin Koerner
 * 
 *         derived from
 *         http://101companies.org/index.php/101implementation:javaLexer
 * 
 */
public class DGTTMParser {
	private DGTTMRecognizer recognizer;
	private String lexeme = new String();
	boolean lastLineWasAHeader;
	boolean isString;
	private DGTTMToken current;
	// private DGTTMToken previous;
	private Writer writer;
	private ArrayList<File> fileList;

	public DGTTMParser(ArrayList<File> fileList, String path)
			throws FileNotFoundException {
		this.fileList = fileList;
		this.writer = new OutputStreamWriter(new FileOutputStream(path));
	}

	public void parse() throws IOException {
		for (File f : this.fileList) {
			this.recognizer = new DGTTMRecognizer(f);
			// writer.write(f.toString());
			// writer.write("\n");
			this.reset();
			while (this.recognizer.hasNext()) {
				this.read();
				if (this.current == BODY) {
					while (this.recognizer.hasNext()
							&& this.current != CLOSEDBODY) {
						// inside a textblock
						this.read();
						if (this.current == TUV) {
							while (this.recognizer.hasNext()
									&& this.current != CLOSEDTUV) {
								this.read();
								if (this.current == STRING) {
									// the following if-statement removes words
									// written in all caps
									// if (!this.lexeme.matches(".+[A-Z].*")) {
									this.write(this.lexeme);
									// }
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
								if (this.current == WS) {
									this.write(" ");
								}
								if (this.current == BRACES) {
									while (this.recognizer.hasNext()
											&& this.current != CLOSEDBRACES
											&& this.current != CLOSEDTUV) {
										this.current = this.recognizer.next();
									}
								}
							}
							this.write(" ");
						}
					}
				}
			}
			this.write("\n");// new line after file
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
		// this.previous = null;
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
