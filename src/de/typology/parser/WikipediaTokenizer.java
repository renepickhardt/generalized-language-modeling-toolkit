package de.typology.parser;

import static de.typology.parser.WikipediaToken.ASTERISK;
import static de.typology.parser.WikipediaToken.BRACKET;
import static de.typology.parser.WikipediaToken.CLOSEDBRACKET;
import static de.typology.parser.WikipediaToken.CLOSEDCURLYBRACKET;
import static de.typology.parser.WikipediaToken.CLOSEDSQUAREDBRACKET;
import static de.typology.parser.WikipediaToken.COLON;
import static de.typology.parser.WikipediaToken.COMMA;
import static de.typology.parser.WikipediaToken.CURLYBRACKET;
import static de.typology.parser.WikipediaToken.EHH;
import static de.typology.parser.WikipediaToken.EOF;
import static de.typology.parser.WikipediaToken.EQUALITYSIGN;
import static de.typology.parser.WikipediaToken.EXCLAMATIONMARK;
import static de.typology.parser.WikipediaToken.FULLSTOP;
import static de.typology.parser.WikipediaToken.GREATERTHAN;
import static de.typology.parser.WikipediaToken.HH;
import static de.typology.parser.WikipediaToken.HYPHEN;
import static de.typology.parser.WikipediaToken.LESSTHAN;
import static de.typology.parser.WikipediaToken.LINESEPARATOR;
import static de.typology.parser.WikipediaToken.OTHER;
import static de.typology.parser.WikipediaToken.QUESTIONMARK;
import static de.typology.parser.WikipediaToken.QUOTATIONMARK;
import static de.typology.parser.WikipediaToken.SEMICOLON;
import static de.typology.parser.WikipediaToken.SLASH;
import static de.typology.parser.WikipediaToken.SQUAREDBRACKET;
import static de.typology.parser.WikipediaToken.STRING;
import static de.typology.parser.WikipediaToken.UNDERSCORE;
import static de.typology.parser.WikipediaToken.VERTICALBAR;
import static de.typology.parser.WikipediaToken.WS;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

/**
 * @author Martin Koerner
 * 
 *         derived from
 *         http://101companies.org/index.php/101implementation:javaLexer
 * 
 */
public class WikipediaTokenizer implements Iterator<WikipediaToken> {

	private WikipediaToken token = null; // last token recognized
	private boolean eof = false; // reached end of file
	private Reader reader = null; // input stream
	private int lookahead = 0; // lookahead, if any
	private int[] buffer = new int[1000]; // lexeme buffer
	private int index = 0; // length of lexeme
	private HashSet<String> disambiguations;

	public WikipediaTokenizer(String wikiInputPath)
			throws FileNotFoundException {
		InputStream input = new FileInputStream(new File(wikiInputPath));

		// TODO: check if new Bzip2 reader really does the job.
		BufferedInputStream in = new BufferedInputStream(input);
		BZip2CompressorInputStream bzIn = null;
		try {
			bzIn = new BZip2CompressorInputStream(in);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.reader = new BufferedReader(new InputStreamReader(bzIn));
		// BZip2InputStream cb = new BZip2InputStream(input, false);
		// this.reader = new BufferedReader(new InputStreamReader(cb));

		// use the following line for reading xml files:
		// this.reader = new BufferedReader(new FileReader(new File(s)));
		this.disambiguations = new HashSet<String>();
		boolean languageSpecified = false;

		// this part declares language specific variables
		if (wikiInputPath.contains("dewiki")) {
			this.disambiguations.add("Begriffsklärung");
			this.disambiguations.add("begriffsklärung");
			System.out.println("this is a german wikipedia xml");
			languageSpecified = true;
		}

		if (wikiInputPath.contains("barwiki")) {
			this.disambiguations.add("Begriffsklärung");
			this.disambiguations.add("begriffsklärung");
			System.out.println("this is a bavarian wikipedia xml");
			languageSpecified = true;
		}

		if (wikiInputPath.contains("enwiki")) {
			this.disambiguations.add("Disambiguation");
			this.disambiguations.add("disambiguation");
			this.disambiguations.add("Disambig");
			this.disambiguations.add("disambig");
			this.disambiguations.add("Geodis");
			this.disambiguations.add("geodis");
			// geographical location name disambiguation pages
			this.disambiguations.add("Hndis");
			this.disambiguations.add("hndis");
			// Human name disambiguation pages
			this.disambiguations.add("Roadindex");
			this.disambiguations.add("roadindex");
			// Street name disambiguation pages
			this.disambiguations.add("Shipindex");
			this.disambiguations.add("shipindex");
			// ship name disambiguation pages
			System.out.println("this is a english wikipedia xml");
			languageSpecified = true;
		}

		if (wikiInputPath.contains("eswiki")) {
			this.disambiguations.add("Desambiguación");
			this.disambiguations.add("desambiguación");
			this.disambiguations.add("Homonimia");
			this.disambiguations.add("homonimia");
			this.disambiguations.add("Idénticos");
			this.disambiguations.add("idénticos");
			System.out.println("this is a spanish wikipedia xml");
			languageSpecified = true;
		}

		if (wikiInputPath.contains("frwiki")) {
			this.disambiguations.add("Homonymie");
			this.disambiguations.add("homonymie");
			this.disambiguations.add("Patronymie");
			this.disambiguations.add("patronymie");
			this.disambiguations.add("Homonymes");
			this.disambiguations.add("homonymes");
			this.disambiguations.add("Toponymie");
			this.disambiguations.add("toponymie");
			this.disambiguations.add("Abréviation");
			this.disambiguations.add("abréviation");
			System.out.println("this is a french wikipedia xml");
			languageSpecified = true;
		}

		if (wikiInputPath.contains("itwiki")) {
			this.disambiguations.add("Disambigua");
			this.disambiguations.add("disambigua");
			this.disambiguations.add("Omonime");
			this.disambiguations.add("omonime");
			this.disambiguations.add("Mercurio");
			this.disambiguations.add("mercurio");
			this.disambiguations.add("Interprogetto");
			this.disambiguations.add("interprogetto");
			System.out.println("this is a italian wikipedia xml");
			languageSpecified = true;
		}
		if (languageSpecified == false) {
			System.out
					.println("please check naming or declare language specific variables (see WikipediaTokenizer.java)");
		}

	}

	// Extract lexeme from buffer
	public String getLexeme() {
		return new String(this.buffer, 0, this.index);
	}

	public HashSet<String> getdisambiguations() {
		return this.disambiguations;
	}

	// Reset state to begin new token
	private void reset() throws IOException {
		if (this.eof) {
			throw new IllegalStateException();
		}
		this.index = 0;
		this.token = null;
		if (this.lookahead == 0) {
			this.read();
		}
	}

	// Read one more char.
	// Add previous char, if any, to the buffer.
	//
	private void read() throws IOException {
		if (this.eof) {
			throw new IllegalStateException();
		}
		if (this.lookahead != 0) {
			this.buffer[this.index] = this.lookahead;
			this.index++;
			if (this.index == 1000) {
				this.index = 0;
				System.out.println("buffer overflow!");
				// reset buffer if token gets too big (very unlikely to happen)
			}
		}
		this.lookahead = this.reader.read();
	}

	// Recognize a token
	public void lex() throws IOException {
		this.reset();

		// Recognize newline
		if (this.lookahead == '\r') {
			this.read();
			if (this.lookahead == '\n') {
				this.token = LINESEPARATOR;
				this.read();
				return;
			}
			this.token = OTHER;
			return;
		}

		// Recognize newline
		if (this.lookahead == '\n') {
			this.token = LINESEPARATOR;
			this.read();
			return;
		}
		// Recognize whitespace
		if (Character.isWhitespace(this.lookahead)) {
			this.read();
			this.token = WS;
			return;
		}

		// Recognize fullstop
		if (this.lookahead == '.') {
			this.read();
			this.token = FULLSTOP;
			return;
		}

		// Recognize comma
		if (this.lookahead == ',') {
			this.read();
			this.token = COMMA;
			return;
		}
		// Recognize semicolon
		if (this.lookahead == ';') {
			this.read();
			this.token = SEMICOLON;
			return;
		}
		// Recognize colon
		if (this.lookahead == ':') {
			this.read();
			this.token = COLON;
			return;
		}

		// recognize quotation mark
		if (this.lookahead == 39) {// 39='
			this.read();
			this.token = QUOTATIONMARK;
			return;
		}

		// Recognize underscore
		if (this.lookahead == '_') {
			this.read();
			this.token = UNDERSCORE;
			return;
		}
		// Recognize hyphen
		if (this.lookahead == '-') {
			this.read();
			this.token = HYPHEN;
			// Recognize -- as HH
			if (this.lookahead == '-') {
				this.read();
				this.token = HH;
			}
			return;
		}
		// Recognize dash (as hyphen)
		if (this.lookahead == '–') {
			this.read();
			this.token = HYPHEN;
			return;
		}

		// Recognize exclamation mark
		if (this.lookahead == '!') {
			this.read();
			// Recognize !-->
			if (this.lookahead == '-') {
				this.read();
				if (this.lookahead == '-') {
					this.read();
					this.token = EHH;
					return;
				}
				this.token = OTHER;
				return;

			}
			this.token = EXCLAMATIONMARK;
			return;
		}

		// Recognize question mark
		if (this.lookahead == '?') {
			this.read();
			this.token = QUESTIONMARK;
			return;
		}

		// Recognize vertical bar
		if (this.lookahead == '|') {
			this.read();
			this.token = VERTICALBAR;
			return;
		}

		// Recognize slash
		if (this.lookahead == '/') {
			this.read();
			this.token = SLASH;
			return;
		}
		// Recognize asterisk
		if (this.lookahead == '*') {
			this.read();
			this.token = ASTERISK;
			return;
		}
		// Recognize equality sign
		if (this.lookahead == '=') {
			this.read();
			this.token = EQUALITYSIGN;
			return;
		}
		// Recognize end of file
		if (this.lookahead == -1) {
			this.eof = true;
			this.token = EOF;
			return;
		}

		if (this.lookahead == '&') {
			this.read();
			// remove &amp;
			while (this.lookahead == 'a') {
				this.read();
				if (this.lookahead == 'm') {
					this.read();
					if (this.lookahead == 'p') {
						this.read();
						if (this.lookahead == ';') {
							this.read();
						}
					}
				}
			}
			// Recognize and chance &lt; to <
			if (this.lookahead == 'l') {
				this.read();
				if (this.lookahead == 't') {
					this.read();
					if (this.lookahead == ';') {
						this.read();
						this.reset();
						this.buffer[0] = '<';
						this.index++;
						this.token = LESSTHAN;
						return;
					}
				}
				this.token = OTHER;
				return;
			}
			// Recognize and chance &gt; to >
			if (this.lookahead == 'g') {
				this.read();
				if (this.lookahead == 't') {
					this.read();
					if (this.lookahead == ';') {
						this.read();
						this.reset();
						this.buffer[0] = '>';
						this.index++;
						this.token = GREATERTHAN;
						return;
					}
				}
				this.token = OTHER;
				return;
			}

			// // Recognize &nbsp; = whitespace
			if (this.lookahead == 'n') {
				this.read();
				if (this.lookahead == 'b') {
					this.read();
					if (this.lookahead == 's') {
						this.read();
						if (this.lookahead == 'p') {
							this.read();
							if (this.lookahead == ';') {
								this.token = WS;
								this.read();
								return;
							}
						}
					}
				}
				this.token = OTHER;
				return;
			}
			// Recognize &quot; = " (as other)
			if (this.lookahead == 'q') {
				this.read();
				if (this.lookahead == 'u') {
					this.read();
					if (this.lookahead == 'o') {
						this.read();
						if (this.lookahead == 't') {
							this.read();
							if (this.lookahead == ';') {
								this.token = OTHER;
								this.read();
								return;
							}
						}
					}
				}
				this.token = OTHER;
				return;
			}

			this.token = OTHER;
			return;
		}

		// Recognize squared bracket open
		if (this.lookahead == '[') {
			this.read();
			this.token = SQUAREDBRACKET;
			return;
		}

		// Recognize squared bracket close
		if (this.lookahead == ']') {
			this.read();
			this.token = CLOSEDSQUAREDBRACKET;
			return;
		}

		// Recognize bracket open
		if (this.lookahead == '(') {
			this.read();
			this.token = BRACKET;
			return;
		}

		// Recognize bracket close
		if (this.lookahead == ')') {
			this.read();
			this.token = CLOSEDBRACKET;
			return;
		}
		// Recognize curly bracket open
		if (this.lookahead == '{') {
			this.read();
			this.token = CURLYBRACKET;
			return;
		}

		// Recognize curly bracket close
		if (this.lookahead == '}') {
			this.read();
			this.token = CLOSEDCURLYBRACKET;
			return;
		}
		// Recognize lower than sign
		if (this.lookahead == '<') {
			this.read();
			this.token = LESSTHAN;
			return;
		}

		// Recognize curly bracket close
		if (this.lookahead == '>') {
			this.read();
			this.token = GREATERTHAN;
			return;
		}

		// Recognize String
		if (Character.isLetterOrDigit(this.lookahead)) {
			do {
				this.read();
			} while (!Character.isWhitespace(this.lookahead)
					&& Character.isLetterOrDigit(this.lookahead));

			this.token = STRING;
			return;
		}

		// Recognize other
		if (!Character.isLetterOrDigit(this.lookahead)) {

			this.read();

			this.token = OTHER;
			return;
		}

		throw new RecognitionException("Recognizer giving up at "
				+ this.lookahead);
	}

	@Override
	public boolean hasNext() {
		if (this.token != null) {
			return true;
		}
		if (this.eof) {
			return false;
		}
		try {
			this.lex();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	@Override
	public WikipediaToken next() {
		if (this.hasNext()) {
			WikipediaToken result = this.token;
			this.token = null;
			return result;
		} else {
			throw new IllegalStateException();
		}
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	// Stress test: lex until end-of-file
	public void lexall() {
		while (this.hasNext()) {
			WikipediaToken t = this.next();
			System.out.println(t + " : " + this.getLexeme());
		}
	}
}
