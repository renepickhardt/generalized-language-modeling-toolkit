package de.typology.lexerParser;

import static de.typology.lexerParser.ReutersToken.BRACES;
import static de.typology.lexerParser.ReutersToken.CLOSEDBRACES;
import static de.typology.lexerParser.ReutersToken.CLOSEDP;
import static de.typology.lexerParser.ReutersToken.CLOSEDTEXT;
import static de.typology.lexerParser.ReutersToken.COMMA;
import static de.typology.lexerParser.ReutersToken.FULLSTOP;
import static de.typology.lexerParser.ReutersToken.HYPHEN;
import static de.typology.lexerParser.ReutersToken.P;
import static de.typology.lexerParser.ReutersToken.STRING;
import static de.typology.lexerParser.ReutersToken.TEXT;
import static de.typology.lexerParser.ReutersToken.WS;

import java.io.File;
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
public class ReutersParser {
	public static void main(String[] args) throws IOException {

		Writer writer = new OutputStreamWriter(new FileOutputStream(
				Config.get().parsedReutersOutputPath));
		File dir = new File(Config.get().reutersXmlPath);
		File[] fileList = dir.listFiles();
		for (File f : fileList) {
			ReutersRecognizer recognizer = new ReutersRecognizer(f);
			writer.write(f.toString());
			writer.write("\n");
			ReutersToken current = null;
			// ReutersToken previous = null;
			String lexeme = null;
			while (recognizer.hasNext()) {
				// previous = current;
				current = recognizer.next();
				lexeme = recognizer.getLexeme();
				if (current == TEXT) {
					while (recognizer.hasNext() && current != CLOSEDTEXT) {
						// inside a textblock
						// previous = current;
						current = recognizer.next();
						lexeme = recognizer.getLexeme();
						if (current == P) {
							while (recognizer.hasNext() && current != CLOSEDP) {
								current = recognizer.next();
								lexeme = recognizer.getLexeme();

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

								if (current == WS) {
									writer.write(" ");
								}
								if (current == BRACES) {
									while (recognizer.hasNext()
											&& current != CLOSEDBRACES
											&& current != CLOSEDP) {
										current = recognizer.next();
									}
								}

							}
							writer.write("\n");// new line after arcticle
						}
					}
				}
			}
			writer.write("\n");// new line after page
		}
		writer.close();
	}
}
