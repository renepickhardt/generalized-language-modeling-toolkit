package de.typology.lexerParser;

import static de.typology.lexerParser.DGTTMToken.BODY;
import static de.typology.lexerParser.DGTTMToken.BRACES;
import static de.typology.lexerParser.DGTTMToken.CLOSEDBODY;
import static de.typology.lexerParser.DGTTMToken.CLOSEDBRACES;
import static de.typology.lexerParser.DGTTMToken.CLOSEDTUV;
import static de.typology.lexerParser.DGTTMToken.COMMA;
import static de.typology.lexerParser.DGTTMToken.FULLSTOP;
import static de.typology.lexerParser.DGTTMToken.HYPHEN;
import static de.typology.lexerParser.DGTTMToken.LINESEPARATOR;
import static de.typology.lexerParser.DGTTMToken.SEG;
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
import java.util.HashSet;

import de.typology.utils.Config;

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
	private DGTTMToken previous;
	private Writer writer;
	private ArrayList<File> fileList;

	public DGTTMParser(ArrayList<File> fileList) throws FileNotFoundException {
		this.fileList = fileList;
		this.writer = new OutputStreamWriter(new FileOutputStream(
				Config.get().parsedDGTTMOutputPath));
	}

	public void parse() throws IOException {
		HashSet<String> keywords = new HashSet<String>();
		keywords.add("Seite");
		keywords.add("vom");
		keywords.add("Artikel");
		keywords.add("Anhang");
		keywords.add("in");
		keywords.add("Teil");
		keywords.add("Nummer");
		keywords.add("Liste");
		keywords.add("in Anhang");
		keywords.add("ABl");

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
								if (this.current == STRING
										&& this.previous == SEG) {
									if (keywords.contains(this.lexeme)) {
										while (this.recognizer.hasNext()
												&& this.current != LINESEPARATOR
												&& this.current != CLOSEDTUV) {
											this.skip();
										}
									}
								}
								if (this.current == STRING) {
									if (!this.lexeme.matches(".+[A-Z].*")) {
										this.write(this.lexeme);
									}
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
											&& this.current != CLOSEDTUV) {
										this.current = this.recognizer.next();
									}
								}

							}
							this.write("\n");// new line after segment
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
			this.previous = this.current;
			this.current = this.recognizer.next();
			this.lexeme = this.recognizer.getLexeme();
		} else {
			throw new IllegalStateException();
		}
	}

	public void reset() {
		this.lexeme = "";
		this.current = null;
		this.previous = null;
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
