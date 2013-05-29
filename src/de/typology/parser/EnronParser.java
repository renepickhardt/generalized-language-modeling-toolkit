package de.typology.parser;

import static de.typology.parser.Token.AT;
import static de.typology.parser.Token.CLOSEDROUNDBRACKET;
import static de.typology.parser.Token.COLON;
import static de.typology.parser.Token.COMMA;
import static de.typology.parser.Token.EQUALITYSIGN;
import static de.typology.parser.Token.EXCLAMATIONMARK;
import static de.typology.parser.Token.FULLSTOP;
import static de.typology.parser.Token.HEADER;
import static de.typology.parser.Token.HYPHEN;
import static de.typology.parser.Token.LINESEPARATOR;
import static de.typology.parser.Token.QUESTIONMARK;
import static de.typology.parser.Token.QUOTATIONMARK;
import static de.typology.parser.Token.ROUNDBRACKET;
import static de.typology.parser.Token.SEMICOLON;
import static de.typology.parser.Token.SLASH;
import static de.typology.parser.Token.STRING;
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
public class EnronParser {
	private EnronTokenizer recognizer;
	private String lexeme = new String();
	boolean lastLineWasAHeader;
	boolean isString;
	private Token current;
	private Token previous;
	private Writer writer;
	private ArrayList<File> fileList;

	public EnronParser(ArrayList<File> fileList, String output)
			throws FileNotFoundException {
		this.fileList = fileList;
		this.writer = IOHelper.openWriteFile(output, 32 * 1024 * 1024);

	}

	public void parse() throws IOException {
		for (File f : this.fileList) {
			this.recognizer = new EnronTokenizer(f);
			this.write(f.toString());
			this.write("\n");
			this.lastLineWasAHeader = false;
			while (this.recognizer.hasNext()) {
				this.read();

				// Remove header
				if (this.current == HEADER) {
					while (this.current != LINESEPARATOR
							&& this.recognizer.hasNext()) {
						this.read();
					}
					this.lastLineWasAHeader = true;
				}

				// Remove word wraps inside a header
				if (this.lastLineWasAHeader == true
						&& this.previous == LINESEPARATOR && this.current == WS) {
					while (this.current != LINESEPARATOR
							&& this.recognizer.hasNext()) {
						this.read();
					}
				}

				// Remove lines that start with hyphen
				if (this.current == HYPHEN && this.previous == LINESEPARATOR) {
					while (this.current != LINESEPARATOR
							&& this.recognizer.hasNext()) {
						this.read();
					}
				}

				// Remove upper case strings
				if (this.current == STRING) {
					if (!this.lexeme.matches(".+[A-Z].*")) {
						this.write(this.lexeme);
					}
				}
				if (this.current == LINESEPARATOR) {
					this.write("\n");
					this.lastLineWasAHeader = false;
				}
				if (this.current == FULLSTOP) {
					this.write(".");
				}
				if (this.current == COMMA) {
					this.write(",");
				}
				if (this.current == SEMICOLON) {
					this.write(";");
				}
				if (this.current == QUESTIONMARK) {
					this.write("?");
				}
				if (this.current == EXCLAMATIONMARK) {
					this.write("!");
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
				if (this.current == EQUALITYSIGN) {
					this.write("=");
				}
				if (this.current == SLASH) {
					this.write("/");
				}
				if (this.current == COLON) {
					this.write(":");
				}
				if (this.current == QUOTATIONMARK) {
					this.write("'");
				}
				if (this.current == ROUNDBRACKET) {
					while (this.recognizer.hasNext()
							&& this.current != CLOSEDROUNDBRACKET) {
						this.read();
					}
				}

			}
			this.write("\n");
			this.write("<ENDOFMAIL>");// marks end of the file
			this.write("\n");
			this.writer.flush();
			this.recognizer.close();
		}
		this.writer.close();
	}

	public void read() {
		if (this.recognizer.hasNext()) {
			this.recognizer.lex();
			this.previous = this.current;
			this.current = this.recognizer.next();
			this.lexeme = this.recognizer.getLexeme();
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
