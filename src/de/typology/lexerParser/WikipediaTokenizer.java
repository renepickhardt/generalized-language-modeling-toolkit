package de.typology.lexerParser;

import static de.typology.lexerParser.WikipediaToken.ASTERISK;
import static de.typology.lexerParser.WikipediaToken.BRACKET;
import static de.typology.lexerParser.WikipediaToken.CLOSEDBRACKET;
import static de.typology.lexerParser.WikipediaToken.CLOSEDCURLYBRACKET;
import static de.typology.lexerParser.WikipediaToken.CLOSEDSQUAREDBRACKET;
import static de.typology.lexerParser.WikipediaToken.COLON;
import static de.typology.lexerParser.WikipediaToken.COMMA;
import static de.typology.lexerParser.WikipediaToken.CURLYBRACKET;
import static de.typology.lexerParser.WikipediaToken.EHH;
import static de.typology.lexerParser.WikipediaToken.EOF;
import static de.typology.lexerParser.WikipediaToken.EQUALITYSIGN;
import static de.typology.lexerParser.WikipediaToken.EXCLAMATIONMARK;
import static de.typology.lexerParser.WikipediaToken.FULLSTOP;
import static de.typology.lexerParser.WikipediaToken.GREATERTHAN;
import static de.typology.lexerParser.WikipediaToken.HH;
import static de.typology.lexerParser.WikipediaToken.HYPHEN;
import static de.typology.lexerParser.WikipediaToken.LESSTHAN;
import static de.typology.lexerParser.WikipediaToken.LINESEPARATOR;
import static de.typology.lexerParser.WikipediaToken.OTHER;
import static de.typology.lexerParser.WikipediaToken.QUESTIONMARK;
import static de.typology.lexerParser.WikipediaToken.QUOTATIONMARK;
import static de.typology.lexerParser.WikipediaToken.SEMICOLON;
import static de.typology.lexerParser.WikipediaToken.SLASH;
import static de.typology.lexerParser.WikipediaToken.SQUAREDBRACKET;
import static de.typology.lexerParser.WikipediaToken.STRING;
import static de.typology.lexerParser.WikipediaToken.UNDERSCORE;
import static de.typology.lexerParser.WikipediaToken.VERTICALBAR;
import static de.typology.lexerParser.WikipediaToken.WS;

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

import org.itadaki.bzip2.BZip2InputStream;

import de.typology.utils.Config;

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

	public WikipediaTokenizer(String s) throws FileNotFoundException {
		InputStream input = new FileInputStream(new File(s));
		BZip2InputStream cb = new BZip2InputStream(input, false);
		this.reader = new BufferedReader(new InputStreamReader(cb));
		// use the following line for reading xml files:
		// this.reader = new BufferedReader(new FileReader(new File(s)));
		this.disambiguations = new HashSet<String>();
		boolean languageSpecified = false;

		// this part declares language specific variables
		if (Config.get().wikiXmlPath.contains("dewiki")) {
			this.disambiguations.add("Begriffsklärung");
			this.disambiguations.add("begriffsklärung");
			System.out.println("This is a german wikipedia XML.");
			languageSpecified = true;
		}

		if (Config.get().wikiXmlPath.contains("enwiki")) {
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
			System.out.println("This is a english wikipedia XML.");
			languageSpecified = true;
		}

		if (Config.get().wikiXmlPath.contains("eswiki")) {
			this.disambiguations.add("Desambiguación");
			this.disambiguations.add("desambiguación");
			this.disambiguations.add("Homonimia");
			this.disambiguations.add("homonimia");
			this.disambiguations.add("Idénticos");
			this.disambiguations.add("idénticos");
			System.out.println("This is a spanish wikipedia XML.");
			languageSpecified = true;
		}

		if (Config.get().wikiXmlPath.contains("frwiki")) {
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
			System.out.println("This is a french wikipedia XML.");
			languageSpecified = true;
		}

		if (Config.get().wikiXmlPath.contains("itwiki")) {
			this.disambiguations.add("Disambigua");
			this.disambiguations.add("disambigua");
			this.disambiguations.add("Omonime");
			this.disambiguations.add("omonime");
			this.disambiguations.add("Mercurio");
			this.disambiguations.add("mercurio");
			this.disambiguations.add("Interprogetto");
			this.disambiguations.add("interprogetto");
			System.out.println("This is a italian wikipedia XML.");
			languageSpecified = true;
		}
		if (languageSpecified == false) {
			System.out
					.println("Please check naming or declare language specific variables (see WikipediaTokenizer.java)");
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

		// Recognize semicolon
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
		// Recognize quotation mark
		if (this.lookahead == '\'') {
			this.read();
			this.token = QUOTATIONMARK;
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
		// Recognize colon
		if (this.lookahead == ':') {
			this.read();
			this.token = COLON;
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
