package de.typology.lexerParser;

import static de.typology.lexerParser.WikipediaToken.ASTERISK;
import static de.typology.lexerParser.WikipediaToken.BRACKET;
import static de.typology.lexerParser.WikipediaToken.CLOSEDBRACKET;
import static de.typology.lexerParser.WikipediaToken.CLOSEDCURLYBRACKET;
import static de.typology.lexerParser.WikipediaToken.CLOSEDEHH;
import static de.typology.lexerParser.WikipediaToken.CLOSEDIMAGEMAP;
import static de.typology.lexerParser.WikipediaToken.CLOSEDREF;
import static de.typology.lexerParser.WikipediaToken.CLOSEDSQUAREDBRACKET;
import static de.typology.lexerParser.WikipediaToken.CLOSEDTEXT;
import static de.typology.lexerParser.WikipediaToken.COLON;
import static de.typology.lexerParser.WikipediaToken.COMMA;
import static de.typology.lexerParser.WikipediaToken.CURLYBRACKET;
import static de.typology.lexerParser.WikipediaToken.EHH;
import static de.typology.lexerParser.WikipediaToken.EQUALITYSIGN;
import static de.typology.lexerParser.WikipediaToken.FULLSTOP;
import static de.typology.lexerParser.WikipediaToken.HYPHEN;
import static de.typology.lexerParser.WikipediaToken.IMAGEMAP;
import static de.typology.lexerParser.WikipediaToken.LINESEPARATOR;
import static de.typology.lexerParser.WikipediaToken.OTHER;
import static de.typology.lexerParser.WikipediaToken.REF;
import static de.typology.lexerParser.WikipediaToken.SEMICOLON;
import static de.typology.lexerParser.WikipediaToken.SQUAREDBRACKET;
import static de.typology.lexerParser.WikipediaToken.STRING;
import static de.typology.lexerParser.WikipediaToken.TEXT;
import static de.typology.lexerParser.WikipediaToken.VERTICALBAR;
import static de.typology.lexerParser.WikipediaToken.WS;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import de.typology.utils.Config;

/**
 * @author Martin Koerner
 * 
 *         derived from
 *         http://101companies.org/index.php/101implementation:javaLexer
 * 
 */
public class WikipediaParser {
	public static void main(String[] args) throws IOException {
		WikipediaRecognizer recognizer = new WikipediaRecognizer(
				Config.get().wikiXmlPath);
		Writer writer = new OutputStreamWriter(new FileOutputStream(
				Config.get().parsedWikiOutputPath));

		WikipediaToken current = null;
		WikipediaToken previous = null;
		String lexeme = null;

		// this part declares language specific variables
		String disambiguation = new String();
		String disambiguation2 = new String();
		if (Config.get().wikiXmlPath.contains("dewiki")) {
			disambiguation = "Begriffsklärung";
			System.out.println("This is a german wikipedia XML.");
		} else {
			if (Config.get().wikiXmlPath.contains("enwiki")) {
				disambiguation = "disambig";
				disambiguation2 = "geodis";// see
											// http://en.wikipedia.org/wiki/Template:Geodis
				System.out.println("This is a english wikipedia XML.");
			} else {
				System.out
						.println("Please declare language specific variables (see WikipediaParser.java)");
			}
		}

		String link = "";
		boolean isLink = false;
		int bracketCount = 0;
		int verticalBarCount = 0;
		while (recognizer.hasNext()) {
			previous = current;
			current = recognizer.next();
			lexeme = recognizer.getLexeme();
			if (current == TEXT) {
				while (recognizer.hasNext() && current != CLOSEDTEXT) {
					// inside a textblock
					previous = current;
					current = recognizer.next();
					lexeme = recognizer.getLexeme();

					if (current == OTHER && previous == TEXT) {
						if (lexeme.equals("#") || lexeme.equals("_")) {
							while (recognizer.hasNext()
									&& current != CLOSEDTEXT
									&& current != LINESEPARATOR) {
								current = recognizer.next();
								previous = current;
							}
						}
					}
					if (current == BRACKET) {
						bracketCount = 1;
						while (bracketCount != 0 && recognizer.hasNext()
								&& current != CLOSEDTEXT) {
							current = recognizer.next();
							if (current == BRACKET) {
								bracketCount++;
							}
							if (current == CLOSEDBRACKET) {
								bracketCount--;
							}
						}

					}

					if (current == CURLYBRACKET) {
						bracketCount = 1;
						while (bracketCount != 0 && recognizer.hasNext()
								&& current != CLOSEDTEXT) {
							previous = current;
							current = recognizer.next();
							if (current == CURLYBRACKET) {
								bracketCount++;
							}
							if (current == CLOSEDCURLYBRACKET) {
								bracketCount--;
							}
							if (previous == CURLYBRACKET
									&& current == STRING
									&& (recognizer.getLexeme().contains(
											disambiguation) || recognizer
											.getLexeme().contains(
													disambiguation2))) {
								writer.write("<DISAMBIGUATION>");
							}
							if (previous == CURLYBRACKET && current == STRING
									&& recognizer.getLexeme().contains("TOC")) {
								writer.write("<TOC>");
							}
							// Recognize {{Audio|...}}
							if (current == STRING
									&& recognizer.getLexeme().contains("Audio")) {
								isLink = true;
							}

							if (current == STRING) {
								link += recognizer.getLexeme();
							}
							if (current == WS) {
								link += " ";
							}
							if (current == HYPHEN) {
								link += "-";
							}
							if (current == VERTICALBAR) {
								// remove part before vertical bar
								link = "";
							}

						}
						if (isLink) {
							writer.write(link);
							isLink = false;
						}
					}

					if (current == SQUAREDBRACKET) {
						link = "";
						isLink = true;
						bracketCount = 1;
						verticalBarCount = 0;
						while (bracketCount != 0 && recognizer.hasNext()
								&& current != CLOSEDTEXT) {
							current = recognizer.next();
							if (current == SQUAREDBRACKET) {
								bracketCount++;
							}
							if (current == CLOSEDSQUAREDBRACKET) {
								bracketCount--;
							}
							if (bracketCount > 2) {
								isLink = false;
							}
							if (bracketCount == 2) {
								// inside a valid link
								if (current == STRING) {
									link += recognizer.getLexeme();
								}
								if (current == WS) {
									link += " ";
								}
								if (current == HYPHEN) {
									link += "-";
								}
								if (current == COLON) {
									isLink = false;
								}
								if (current == VERTICALBAR) {
									// remove part before vertical bar
									link = "";
									verticalBarCount++;
								}
							}

						}
						if (isLink && verticalBarCount < 2) {
							writer.write(link);
							isLink = false;
						}
					}

					if (previous == FULLSTOP && current == LINESEPARATOR) {
						writer.write(" ");
					}

					if (current == STRING) {
						writer.write(lexeme);
					}

					if (current == FULLSTOP) {
						writer.write(lexeme);
					}
					if (current == COMMA) {
						writer.write(lexeme);
					}
					if (current == SEMICOLON) {
						writer.write(lexeme);
					}
					if (current == HYPHEN) {
						writer.write("-");
					}

					if (previous == LINESEPARATOR
							&& (current == EQUALITYSIGN || current == COLON || current == ASTERISK)) {
						// equality sign-->headline, colon or asterisk-->listing
						while (recognizer.hasNext() && current != CLOSEDTEXT
								&& current != LINESEPARATOR) {
							current = recognizer.next();
							previous = current;
						}
					}

					if (current == REF) {
						while (recognizer.hasNext() && current != CLOSEDREF
								&& current != CLOSEDTEXT) {
							current = recognizer.next();
							previous = current;
						}
					}
					if (current == EHH) {
						while (recognizer.hasNext() && current != CLOSEDEHH
								&& current != CLOSEDTEXT) {
							current = recognizer.next();
							previous = current;
						}
					}
					if (current == IMAGEMAP) {
						while (recognizer.hasNext()
								&& current != CLOSEDIMAGEMAP
								&& current != CLOSEDTEXT) {
							current = recognizer.next();
							previous = current;
						}
					}

					if (current == WS && previous != WS) {
						writer.write(" ");
					}
				}
				writer.write("\n");// new line after page
			}
		}
		writer.close();
	}
}
