package de.typology.lexerParser;

import static de.typology.lexerParser.DGTTMToken.BODY;
import static de.typology.lexerParser.DGTTMToken.BRACES;
import static de.typology.lexerParser.DGTTMToken.CLOSEDBODY;
import static de.typology.lexerParser.DGTTMToken.CLOSEDBRACES;
import static de.typology.lexerParser.DGTTMToken.CLOSEDSEG;
import static de.typology.lexerParser.DGTTMToken.CLOSEDTUV;
import static de.typology.lexerParser.DGTTMToken.COMMA;
import static de.typology.lexerParser.DGTTMToken.FULLSTOP;
import static de.typology.lexerParser.DGTTMToken.HYPHEN;
import static de.typology.lexerParser.DGTTMToken.SEG;
import static de.typology.lexerParser.DGTTMToken.STRING;
import static de.typology.lexerParser.DGTTMToken.TUV;
import static de.typology.lexerParser.DGTTMToken.UPPERCASE;
import static de.typology.lexerParser.DGTTMToken.WS;

import java.io.File;
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
	public static void main(String[] args) throws IOException {
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

		Writer writer = new OutputStreamWriter(new FileOutputStream(
				Config.get().parsedDGTTMOutputPath));
		System.out.println("Get list of files.");
		getFileList(new File(Config.get().DGTTMPath));
		System.out.println("Start parsing.");
		for (File f : fileList) {
			DGTTMRecognizer recognizer = new DGTTMRecognizer(f);
			// writer.write(f.toString());
			// writer.write("\n");
			DGTTMToken current = null;
			DGTTMToken previous = null;
			String lexeme = null;
			while (recognizer.hasNext()) {
				previous = current;
				current = recognizer.next();
				lexeme = recognizer.getLexeme();
				if (current == BODY) {
					while (recognizer.hasNext() && current != CLOSEDBODY) {
						// inside a textblock
						previous = current;
						current = recognizer.next();
						lexeme = recognizer.getLexeme();
						if (current == TUV) {
							while (recognizer.hasNext() && current != CLOSEDTUV) {
								previous = current;
								current = recognizer.next();
								lexeme = recognizer.getLexeme();

								// removes lines with uppercase only
								if (previous == SEG && current == UPPERCASE) {
									while (current != CLOSEDSEG
											&& current != STRING) {
										current = recognizer.next();
									}
								}

								// removes lines, that start with a keyword
								if (previous == SEG && current == STRING) {
									if (keywords.contains(lexeme)) {
										while (current != CLOSEDSEG) {
											current = recognizer.next();
										}
									}
								}
								if (current == STRING) {
									writer.write(lexeme);
								}
								if (current == UPPERCASE) {
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
											&& current != CLOSEDTUV) {
										current = recognizer.next();
									}
								}

							}
							writer.write("\n");// new line after segment
						}
					}
				}
			}
			writer.write("\n");// new line after file
		}
		writer.close();
		System.out.println("Done.");
	}

	private static ArrayList<File> fileList = new ArrayList<File>();

	private static void getFileList(File f) {
		File[] files = f.listFiles();
		if (files != null) {
			for (File file : files) {
				if (file.isDirectory()) {
					getFileList(file);
				} else {
					fileList.add(file);
				}
			}
		}
	}
}
