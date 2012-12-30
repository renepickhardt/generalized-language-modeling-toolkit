package de.typology.lexerParser;

import static de.typology.lexerParser.WikipediaToken.ASTERISK;
import static de.typology.lexerParser.WikipediaToken.AUDIO;
import static de.typology.lexerParser.WikipediaToken.BRACKET;
import static de.typology.lexerParser.WikipediaToken.CLOSEDBRACKET;
import static de.typology.lexerParser.WikipediaToken.CLOSEDCURLYBRACKET;
import static de.typology.lexerParser.WikipediaToken.CLOSEDELEMENT;
import static de.typology.lexerParser.WikipediaToken.CLOSEDREF;
import static de.typology.lexerParser.WikipediaToken.CLOSEDSQUAREDBRACKET;
import static de.typology.lexerParser.WikipediaToken.CLOSEDTEXT;
import static de.typology.lexerParser.WikipediaToken.COLON;
import static de.typology.lexerParser.WikipediaToken.COMMA;
import static de.typology.lexerParser.WikipediaToken.CURLYBRACKET;
import static de.typology.lexerParser.WikipediaToken.EHH;
import static de.typology.lexerParser.WikipediaToken.ELEMENT;
import static de.typology.lexerParser.WikipediaToken.EQUALITYSIGN;
import static de.typology.lexerParser.WikipediaToken.EXCLAMATIONMARK;
import static de.typology.lexerParser.WikipediaToken.FULLSTOP;
import static de.typology.lexerParser.WikipediaToken.GREATERTHAN;
import static de.typology.lexerParser.WikipediaToken.HH;
import static de.typology.lexerParser.WikipediaToken.HYPHEN;
import static de.typology.lexerParser.WikipediaToken.LINESEPARATOR;
import static de.typology.lexerParser.WikipediaToken.LINK;
import static de.typology.lexerParser.WikipediaToken.OTHER;
import static de.typology.lexerParser.WikipediaToken.QUESTIONMARK;
import static de.typology.lexerParser.WikipediaToken.QUOTATIONMARK;
import static de.typology.lexerParser.WikipediaToken.REF;
import static de.typology.lexerParser.WikipediaToken.SEMICOLON;
import static de.typology.lexerParser.WikipediaToken.SQUAREDBRACKET;
import static de.typology.lexerParser.WikipediaToken.STRING;
import static de.typology.lexerParser.WikipediaToken.TEXT;
import static de.typology.lexerParser.WikipediaToken.UNDERSCORE;
import static de.typology.lexerParser.WikipediaToken.VERTICALBAR;
import static de.typology.lexerParser.WikipediaToken.WS;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;

import de.typology.utils.IOHelper;

/**
 * @author Martin Koerner
 * 
 *         derived from
 *         http://101companies.org/index.php/101implementation:javaLexer
 * 
 */
public class WikipediaParser {
	private WikipediaRecognizer recognizer;
	private String lexeme = new String();
	private int bracketCount;
	private int verticalBarCount;
	// private String link;
	private String linkLabel;
	private WikipediaToken label;
	private WikipediaToken current;
	private WikipediaToken previous;
	private Writer writer;
	private HashSet<String> disambiguations;

	public WikipediaParser(WikipediaRecognizer recognizer, String output)
			throws FileNotFoundException {
		this.recognizer = recognizer;
		this.writer = IOHelper.openWriteFile(output, 32 * 1024 * 1024);
		this.disambiguations = recognizer.getTokenizer().getdisambiguations();
	}

	public void parse() throws IOException {
		while (this.recognizer.hasNext()) {
			this.read();
			// System.out.println(this.current + " : " + this.lexeme);
			// try {
			// Thread.sleep(1);
			// } catch (InterruptedException e) {
			// e.printStackTrace();
			// }

			if (this.current == TEXT) {
				while (this.current != CLOSEDTEXT) {
					this.label = null;
					this.read();
					// System.out.println(this.current + " : " + this.lexeme);
					// try {
					// Thread.sleep(20);
					// } catch (InterruptedException e) {
					// e.printStackTrace();
					// }

					// Remove lines at the beginning of text starting with # or
					// _
					if (this.current == OTHER && this.previous == TEXT) {
						if (this.lexeme.equals("#") || this.lexeme.equals("_")) {
							while (this.recognizer.hasNext()
									&& this.current != CLOSEDTEXT
									&& this.current != LINESEPARATOR) {
								this.read();
							}
						}
					}

					// Remove headlines and listings
					if (this.previous == LINESEPARATOR
							&& (this.current == EQUALITYSIGN
									|| this.current == COLON
									|| this.current == ASTERISK
									|| this.current == SEMICOLON
									|| this.current == UNDERSCORE
									|| this.current == EXCLAMATIONMARK || this.current == VERTICALBAR)) {
						// equality sign or semicolon-->headline, colon or
						// asterisk-->listing
						while (this.current != CLOSEDTEXT
								&& this.current != LINESEPARATOR) {
							this.read();
						}
					}

					if (this.previous == FULLSTOP
							&& this.current == LINESEPARATOR) {
						this.write(" ");
					}
					if (this.current == STRING) {
						this.write(this.lexeme);
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
					if (this.current == AUDIO) {
						this.write(this.lexeme);
					}

					if (this.current == LINESEPARATOR) {
						this.write(" ");
					}

					// Recognize <ref>...</ref> inside a text block
					if (this.current == REF) {
						while (this.current != CLOSEDREF
								&& this.current != CLOSEDTEXT) {
							this.read();
						}
					}

					// Recognize <...>...</...> inside a text block
					if (this.current == ELEMENT) {
						while (this.current != CLOSEDELEMENT
								&& this.current != CLOSEDTEXT) {
							this.read();
						}
					}

					// Recognize <!--...--> inside a text block
					if (this.current == EHH) {
						while (this.current != GREATERTHAN
								&& this.previous != HH
								&& this.current != CLOSEDTEXT) {
							this.read();
						}
					}

					// Recognize (...)
					if (this.current == BRACKET) {
						this.bracketCount = 1;
						while (this.bracketCount != 0
								&& this.current != CLOSEDTEXT) {
							this.read();
							if (this.current == BRACKET) {
								this.bracketCount++;
							}
							if (this.current == CLOSEDBRACKET) {
								this.bracketCount--;
							}
						}
					}

					// Recognize {...}
					if (this.current == CURLYBRACKET) {
						this.linkLabel = "";
						this.bracketCount = 1;
						while (this.bracketCount != 0
								&& this.current != CLOSEDTEXT) {
							this.read();
							if (this.current == CURLYBRACKET) {
								this.bracketCount++;
							}
							if (this.current == CLOSEDCURLYBRACKET) {
								this.bracketCount--;
							}

							if (this.current == STRING
									&& this.disambiguations
											.contains(this.lexeme)) {
								this.writer.write("<DISAMBIGUATION>");
							}
							if (this.previous == CURLYBRACKET
									&& this.current == STRING
									&& this.lexeme.contains("TOC")) {
								this.writer.write("<TOC>");
							}
							if (this.previous == CURLYBRACKET
									&& this.current == STRING
									&& this.lexeme.contains("Wikipedia")) {
								this.writer.write("<DISAMBIGUATION>");
							}
							if (this.current == STRING) {
								this.linkLabel += this.lexeme;
							}
							if (this.current == WS) {
								this.linkLabel += " ";
							}
							if (this.current == QUESTIONMARK) {
								this.linkLabel += "?";
							}
							if (this.current == EXCLAMATIONMARK) {
								this.linkLabel += "!";
							}
							if (this.current == HYPHEN) {
								this.linkLabel += "-";
							}
							// Recognize {{Audio|...}}
							if (this.previous == CURLYBRACKET
									&& this.lexeme.equals("Audio")) {
								this.label = AUDIO;
							}
							if (this.label == AUDIO
									&& this.current == VERTICALBAR) {
								this.linkLabel = "";
							}
						}
						if (this.bracketCount == 0) {
							if (this.label == AUDIO) {
								this.writer.write(this.linkLabel);
							}
						}

					}
					// Recognize [...]
					if (this.current == SQUAREDBRACKET) {
						this.bracketCount = 1;
						this.verticalBarCount = 0;
						// this.link = "";
						this.linkLabel = "";
						while (this.bracketCount != 0
								&& this.current != CLOSEDTEXT) {
							this.read();
							if (this.current == SQUAREDBRACKET) {
								this.bracketCount++;
							}
							if (this.current == CLOSEDSQUAREDBRACKET) {
								this.bracketCount--;
							}
							if (this.bracketCount > 2) {
								this.label = OTHER;
							}
							if (this.bracketCount == 2 && this.label != OTHER) {
								this.label = LINK;
								// inside a valid link
								if (this.current == STRING) {
									this.linkLabel += this.lexeme;
								}
								if (this.current == WS) {
									this.linkLabel += " ";
								}
								if (this.current == HYPHEN) {
									this.linkLabel += "-";
								}
								if (this.current == VERTICALBAR) {
									this.verticalBarCount++;
									// this.link = this.lexeme.substring(2,
									// this.lexeme.length() - 1);
									this.linkLabel = "";
								}
								if (this.current == COLON) {
									// Recognize [[lang:language]]
									this.label = OTHER;
								}
							}
						}
						if (this.label == LINK && this.bracketCount == 0
								&& this.verticalBarCount < 2) {
							this.write(this.linkLabel);
							// this could be usefull for building the
							// WikipediaLinkExtractor
							// if (this.label == LINK) {
							// if (this.verticalBarCount == 1) {
							// LABELEDLINK;
							// } else {
							// LINK;
							// }
							// }
						}
					}

				}
				this.write("\n");// new line after page
				this.writer.flush();
			}
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

	public void write(String s) {
		try {
			this.writer.write(s);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
