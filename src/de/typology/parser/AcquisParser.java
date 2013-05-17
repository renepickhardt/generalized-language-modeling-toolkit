package de.typology.parser;

import static de.typology.parser.Token.BODY;
import static de.typology.parser.Token.CLOSEDBODY;
import static de.typology.parser.Token.CLOSEDROUNDBRACKET;
import static de.typology.parser.Token.CLOSEDTUV;
import static de.typology.parser.Token.COLON;
import static de.typology.parser.Token.COMMA;
import static de.typology.parser.Token.EXCLAMATIONMARK;
import static de.typology.parser.Token.FULLSTOP;
import static de.typology.parser.Token.HYPHEN;
import static de.typology.parser.Token.QUESTIONMARK;
import static de.typology.parser.Token.QUOTATIONMARK;
import static de.typology.parser.Token.ROUNDBRACKET;
import static de.typology.parser.Token.SEMICOLON;
import static de.typology.parser.Token.STRING;
import static de.typology.parser.Token.TUV;
import static de.typology.parser.Token.WS;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

import de.typology.utils.Config;
import de.typology.utils.IOHelper;

/**
 * @author Martin Koerner
 * 
 *         derived from
 *         http://101companies.org/index.php/101implementation:javaLexer
 * 
 */
public class AcquisParser {
	private AcquisTokenizer tokenizer;
	private String lexeme = new String();
	boolean lastLineWasAHeader;
	boolean isString;
	private Token current;
	// private DGTTMToken previous;
	private Writer writer;
	private ArrayList<File> fileList;
	private String dgttmLanguage;

	public AcquisParser(ArrayList<File> fileList, String output,
			String dgttmLanguage) {
		this.dgttmLanguage = dgttmLanguage;
		this.fileList = fileList;
		this.writer = IOHelper.openWriteFile(output,
				Config.get().memoryLimitForWritingFiles);
	}

	public void parse() {
		for (File f : this.fileList) {
			this.tokenizer = new AcquisTokenizer(f, this.dgttmLanguage);
			// writer.write(f.toString());
			// writer.write("\n");
			this.reset();
			while (this.tokenizer.hasNext()) {
				this.read();
				if (this.current == BODY) {
					while (this.tokenizer.hasNext()
							&& this.current != CLOSEDBODY) {
						// inside a textblock
						this.read();
						if (this.current == TUV) {
							while (this.tokenizer.hasNext()
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
								if (this.current == ROUNDBRACKET) {
									while (this.tokenizer.hasNext()
											&& this.current != CLOSEDROUNDBRACKET
											&& this.current != CLOSEDTUV) {
										this.read();
									}
								}
							}
							this.write(" ");
						}
					}
				}
			}
			this.write("\n");// new line after file
			this.tokenizer.close();
		}
		try {
			this.writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void read() {
		if (this.tokenizer.hasNext()) {
			// this.previous = this.current;
			this.tokenizer.lex();
			this.current = this.tokenizer.next();
			this.lexeme = this.tokenizer.getLexeme();
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
		if (this.tokenizer.hasNext()) {
			// this.previous = this.current;
			this.current = this.tokenizer.next();
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
