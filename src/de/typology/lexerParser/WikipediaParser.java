package de.typology.lexerParser;

import static de.typology.lexerParser.WikipediaToken.BRACKET;
import static de.typology.lexerParser.WikipediaToken.CLOSEDBRACKET;
import static de.typology.lexerParser.WikipediaToken.CLOSEDCURLYBRACKET;
import static de.typology.lexerParser.WikipediaToken.CLOSEDTEXT;
import static de.typology.lexerParser.WikipediaToken.COMMA;
import static de.typology.lexerParser.WikipediaToken.CURLYBRACKET;
import static de.typology.lexerParser.WikipediaToken.FULLSTOP;
import static de.typology.lexerParser.WikipediaToken.HYPHEN;
import static de.typology.lexerParser.WikipediaToken.LABELEDLINK;
import static de.typology.lexerParser.WikipediaToken.LINESEPARATOR;
import static de.typology.lexerParser.WikipediaToken.LINK;
import static de.typology.lexerParser.WikipediaToken.OTHER;
import static de.typology.lexerParser.WikipediaToken.QUOTATIONMARK;
import static de.typology.lexerParser.WikipediaToken.STRING;
import static de.typology.lexerParser.WikipediaToken.TEXT;
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

					if (current == CURLYBRACKET) {
						int bracketCount = 1;

						while (bracketCount != 0 && recognizer.hasNext()
								&& current != CLOSEDTEXT) {
							current = recognizer.next();
							if (current == CURLYBRACKET) {
								bracketCount++;
							}
							if (current == CLOSEDCURLYBRACKET) {
								bracketCount--;
							}
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
					if (current == HYPHEN) {
						writer.write("-");
					}
					// some pages start with '''Title'''
					if (previous == LINESEPARATOR && current == QUOTATIONMARK) {
						current = recognizer.next();
						previous = current;
					}

					if (previous == LINESEPARATOR && current != STRING) {
						// first token in line has to be a letter
						while (recognizer.hasNext() && current != CLOSEDTEXT
								&& current != LINESEPARATOR) {
							current = recognizer.next();
							previous = current;
						}
					}

					if (current == LINK) {
						// write right part
						writer.write(recognizer.getLexeme().substring(2,
								recognizer.getLexeme().length() - 2));
					}

					if (current == LABELEDLINK && previous != TEXT) {
						// write right part
						String[] splitLabel = recognizer.getLexeme().split(
								"\\|");
						try {
							writer.write(splitLabel[1].substring(0,
									splitLabel[1].length() - 2));
						} catch (StringIndexOutOfBoundsException s) {
							// TODO: fix this...probably substrings too small?
						}
					}

					// if (current == REF) {
					// while (recognizer.hasNext() && current != CLOSEDREF
					// && current != LINESEPARATOR) {
					// current = recognizer.next();
					// previous = current;
					// }
					// }

					if (current == BRACKET) {
						while (recognizer.hasNext() && current != CLOSEDBRACKET
								&& current != CLOSEDTEXT) {
							current = recognizer.next();
						}

					}

					// if (recognizer.hasNext()) {
					// current = recognizer.next();
					// if (recognizer.hasNext()) {
					// current = recognizer.next();
					// if (current == STRING
					// && (recognizer.getLexeme().equals(
					// "Infobox") || recognizer
					// .getLexeme().equals("Taxobox"))) {
					//
					// while (recognizer.hasNext()
					// && current != CLOSEDTEXT
					// && current != LINESEPARATOR) {
					// current = recognizer.next();
					// previous = current;
					// }
					// }
					// }
					// while (recognizer.hasNext()
					// && current != LINESEPARATOR
					// && current != CLOSEDCURLYBRACKET
					// && current != CLOSEDTEXT) {
					// current = recognizer.next();
					// }
					//
					// }

					if (current == WS) {
						writer.write(" ");
					}

				}
				writer.write("\n");// new line after page
			}
		}
		writer.close();
	}
}
