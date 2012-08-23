package de.typology.lexerParser;

import static de.typology.lexerParser.WikipediaToken.ASTERISK;
import static de.typology.lexerParser.WikipediaToken.BRACKET;
import static de.typology.lexerParser.WikipediaToken.CLOSEDBRACKET;
import static de.typology.lexerParser.WikipediaToken.CLOSEDCURLYBRACKET;
import static de.typology.lexerParser.WikipediaToken.CLOSEDEHH;
import static de.typology.lexerParser.WikipediaToken.CLOSEDIMAGEMAP;
import static de.typology.lexerParser.WikipediaToken.CLOSEDPAGE;
import static de.typology.lexerParser.WikipediaToken.CLOSEDREF;
import static de.typology.lexerParser.WikipediaToken.CLOSEDSQUAREDBRACKET;
import static de.typology.lexerParser.WikipediaToken.CLOSEDTEXT;
import static de.typology.lexerParser.WikipediaToken.CLOSEDTITLE;
import static de.typology.lexerParser.WikipediaToken.COLON;
import static de.typology.lexerParser.WikipediaToken.COMMA;
import static de.typology.lexerParser.WikipediaToken.CURLYBRACKET;
import static de.typology.lexerParser.WikipediaToken.EHH;
import static de.typology.lexerParser.WikipediaToken.EOF;
import static de.typology.lexerParser.WikipediaToken.EQUALITYSIGN;
import static de.typology.lexerParser.WikipediaToken.EXCLAMATIONMARK;
import static de.typology.lexerParser.WikipediaToken.FULLSTOP;
import static de.typology.lexerParser.WikipediaToken.HYPHEN;
import static de.typology.lexerParser.WikipediaToken.IMAGEMAP;
import static de.typology.lexerParser.WikipediaToken.LINESEPARATOR;
import static de.typology.lexerParser.WikipediaToken.OTHER;
import static de.typology.lexerParser.WikipediaToken.PAGE;
import static de.typology.lexerParser.WikipediaToken.QUESTIONMARK;
import static de.typology.lexerParser.WikipediaToken.QUOTATIONMARK;
import static de.typology.lexerParser.WikipediaToken.REF;
import static de.typology.lexerParser.WikipediaToken.SEMICOLON;
import static de.typology.lexerParser.WikipediaToken.SQUAREDBRACKET;
import static de.typology.lexerParser.WikipediaToken.STRING;
import static de.typology.lexerParser.WikipediaToken.TEXT;
import static de.typology.lexerParser.WikipediaToken.TITLE;
import static de.typology.lexerParser.WikipediaToken.URI;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.itadaki.bzip2.BZip2InputStream;

/**
 * @author Martin Koerner
 * 
 *         derived from
 *         http://101companies.org/index.php/101implementation:javaLexer
 * 
 */
public class WikipediaRecognizer implements Iterator<WikipediaToken> {

	private WikipediaToken token = null; // last token recognized
	private boolean eof = false; // reached end of file
	private Reader reader = null; // input stream
	private int lookahead = 0; // lookahead, if any
	private int[] buffer = new int[100000]; // lexeme buffer
	private int index = 0; // length of lexeme
	private String label = "";

	// Keywords to token mapping
	private static Map<String, WikipediaToken> keywords;

	static {
		keywords = new HashMap<String, WikipediaToken>();
		keywords.put("page", PAGE);
		keywords.put("title", TITLE);
		keywords.put("text xml:space=\"preserve\"", TEXT);
		keywords.put("ref", REF);
		keywords.put("!--", EHH);
		keywords.put("imagemap", IMAGEMAP);
		keywords.put("/page", CLOSEDPAGE);
		keywords.put("/title", CLOSEDTITLE);
		keywords.put("/text", CLOSEDTEXT);
		keywords.put("/ref", CLOSEDREF);
		keywords.put("/imagemap", CLOSEDIMAGEMAP);
	}

	public WikipediaRecognizer(String s) throws FileNotFoundException {
		InputStream input = new FileInputStream(new File(s));
		BZip2InputStream cb = new BZip2InputStream(input, false);
		this.reader = new BufferedReader(new InputStreamReader(cb));
		// use the following line for reading xml files:
		// this.reader = new BufferedReader(new FileReader(new File(s)));
	}

	// Extract lexeme from buffer
	public String getLexeme() {
		return new String(this.buffer, 0, this.index);
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
			if (this.index == 100000) {
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
		if (this.lookahead == 13) {
			this.token = LINESEPARATOR;
			this.read();
			if (this.lookahead == 10) {
				this.token = LINESEPARATOR;
				this.read();
			}
			return;
		}

		// Recognize newline
		if (this.lookahead == 10) {
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
		// Recognize hyphen
		if (this.lookahead == '-') {
			this.read();
			this.token = HYPHEN;
			// Recognize --&gt; as CLOSEDEHH
			if (this.lookahead == '-') {
				this.read();
				if (this.lookahead == '&') {
					this.read();
					if (this.lookahead == 'g') {
						this.read();
						if (this.lookahead == 't') {
							this.read();
							if (this.lookahead == ';') {
								this.read();
								this.token = CLOSEDEHH;
								return;
							}
						}
					}
				}
				this.token = OTHER;
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
			this.token = EXCLAMATIONMARK;
			// Recognize !-->
			if (this.lookahead == '-') {
				this.read();
				if (this.lookahead == '-') {
					this.read();
					if (this.lookahead == '&') {
						this.read();
						if (this.lookahead == 'g') {
							this.read();
							if (this.lookahead == 't') {
								this.read();
								if (this.lookahead == ';') {
									this.read();
									this.token = CLOSEDEHH;
									return;
								}
							}
						}
					}
					if (this.lookahead == '>') {
						this.read();
						this.token = CLOSEDEHH;
						return;
					}
				}
				this.token = OTHER;
			}
			return;
		}

		// Recognize question mark
		if (this.lookahead == '?') {
			this.read();
			this.token = QUESTIONMARK;
			return;
		}
		// Recognize quotation mark
		if (this.lookahead == 39) {
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

		// Recognize <???>
		if (this.lookahead == '<') {
			this.read();
			while (this.lookahead != '>') {
				this.read();
				this.label = this.getLexeme().substring(1);
				if (keywords.containsKey(this.label)) {
					this.token = keywords.get(this.label);
					while (this.lookahead != '>') {
						this.read();
					}
					if (this.lookahead == '>') {
						this.read();
					}
					return;
				}
			}
			if (this.lookahead == '>') {
				this.read();
			}
			this.token = OTHER;
			return;
		}

		if (this.lookahead == '&') {
			this.read();
			if (this.lookahead == 'l') {
				this.read();
				if (this.lookahead == 't') {
					this.read();
					if (this.lookahead == ';') {
						//
						this.label = "";
						//
						while (this.lookahead != '&') {
							this.read();
							try {
								this.label = this.getLexeme().substring(4);
								if (keywords.containsKey(this.label)) {
									this.token = keywords.get(this.label);
									while (this.lookahead != '&') {
										this.read();
									}
									return;
								}
							} catch (StringIndexOutOfBoundsException e) {
								// buffer was full
							}

							// Recognizes "<!--"
							if (this.lookahead == '!') {
								this.read();
								if (this.lookahead == '-') {
									this.read();
									if (this.lookahead == '-') {
										this.read();
										this.token = EHH;
										return;
									}
								}
							}
						}
						this.read();
						if (this.lookahead == 'g') {
							this.read();
							if (this.lookahead == 't') {
								this.read();
								if (this.lookahead == ';') {
									this.read();
									// String label =
									// this.getLexeme().substring(
									// 4, this.getLexeme().length() - 4);
									// if (keywords.containsKey(this.label)) {
									// this.token = keywords.get(this.label);
									// } else {
									// this.token = OTHER;
									// }
									return;

								}
							}
						}
					}
				}
			}

			// &nbsp;
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
			}
			// &quot;
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
			}
			// &amp;nbsp;
			if (this.lookahead == 'a') {
				this.read();
				if (this.lookahead == 'm') {
					this.read();
					if (this.lookahead == 'p') {
						this.read();
						if (this.lookahead == ';') {
							this.read();
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
							}
						}
					}
				}
			}
			if (this.lookahead == 'g') {
				this.read();
				if (this.lookahead == 't') {
					this.read();
					if (this.lookahead == ';') {
						this.read();
					}
				}
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

		// Recognize String
		if (Character.isLetterOrDigit(this.lookahead)) {

			// Recognize special String: URI
			if (this.lookahead == 'h') {
				this.read();
				if (this.lookahead == 't') {
					this.read();
					if (this.lookahead == 't') {
						this.read();
						if (this.lookahead == 'p') {
							this.read();
							if (this.lookahead == ':') {
								while (!Character.isWhitespace(this.lookahead)
										&& this.lookahead != '<') {
									this.read();
								}
								this.token = URI;
								return;
							}
						}
					}
				}
			}

			while (!Character.isWhitespace(this.lookahead)
					&& Character.isLetterOrDigit(this.lookahead)) {
				this.read();
			}
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
			throw new RecognitionException(e);
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
