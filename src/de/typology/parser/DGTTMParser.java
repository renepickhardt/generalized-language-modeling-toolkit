package de.typology.parser;

import static de.typology.parser.DGTTMToken.BODY;
import static de.typology.parser.DGTTMToken.BRACES;
import static de.typology.parser.DGTTMToken.CLOSEDBODY;
import static de.typology.parser.DGTTMToken.CLOSEDBRACES;
import static de.typology.parser.DGTTMToken.CLOSEDTUV;
import static de.typology.parser.DGTTMToken.COLON;
import static de.typology.parser.DGTTMToken.COMMA;
import static de.typology.parser.DGTTMToken.EXCLAMATIONMARK;
import static de.typology.parser.DGTTMToken.FULLSTOP;
import static de.typology.parser.DGTTMToken.HYPHEN;
import static de.typology.parser.DGTTMToken.QUESTIONMARK;
import static de.typology.parser.DGTTMToken.QUOTATIONMARK;
import static de.typology.parser.DGTTMToken.SEMICOLON;
import static de.typology.parser.DGTTMToken.STRING;
import static de.typology.parser.DGTTMToken.TUV;
import static de.typology.parser.DGTTMToken.WS;

import java.io.File;
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
public class DGTTMParser {
	private DGTTMRecognizer recognizer;
	private String lexeme = new String();
	boolean lastLineWasAHeader;
	boolean isString;
	private DGTTMToken current;
	// private DGTTMToken previous;
	private Writer writer;
	private ArrayList<File> fileList;
	private String dgttmLanguage;

	public DGTTMParser(ArrayList<File> fileList, String output,
			String dgttmLanguage) {
		this.dgttmLanguage = dgttmLanguage;
		this.fileList = fileList;
		this.writer = IOHelper.openWriteFile(output, 32 * 1024 * 1024);
	}

	public void parse() {
		for (File f : this.fileList) {
			this.recognizer = new DGTTMRecognizer(f, this.dgttmLanguage);
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
								if (this.current == COLON) {
									this.write(": ");
								}
								if (this.current == QUOTATIONMARK) {
									this.write("'");
								}
								if (this.current == HYPHEN) {
									this.write("-");
								}
								if (this.current == WS) {
									this.write(" ");
								}
								if (this.current == QUESTIONMARK) {
									this.write(this.lexeme);
								}
								if (this.current == EXCLAMATIONMARK) {
									this.write(this.lexeme);
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
			try {
				this.writer.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			this.writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void read() {
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
