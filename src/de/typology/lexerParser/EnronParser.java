package de.typology.lexerParser;

import static de.typology.lexerParser.EnronToken.BRACES;
import static de.typology.lexerParser.EnronToken.CLOSEDBRACES;
import static de.typology.lexerParser.EnronToken.COMMA;
import static de.typology.lexerParser.EnronToken.FULLSTOP;
import static de.typology.lexerParser.EnronToken.HEADER;
import static de.typology.lexerParser.EnronToken.HYPHEN;
import static de.typology.lexerParser.EnronToken.LINESEPARATOR;
import static de.typology.lexerParser.EnronToken.STRING;
import static de.typology.lexerParser.EnronToken.WS;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;

import de.typology.utils.Config;

/**
 * @author Martin Koerner
 * 
 *         derived from
 *         http://101companies.org/index.php/101implementation:javaLexer
 * 
 */
public class EnronParser {

	public static void main(String[] args) throws IOException {

		Writer writer = new OutputStreamWriter(new FileOutputStream(
				Config.get().parsedEnronOutputPath));
		System.out.println("Get list of files.");
		getFileList(new File(Config.get().enronPath));
		System.out.println("Start parsing.");
		for (File f : fileList) {
			EnronRecognizer recognizer = new EnronRecognizer(f);
			// writer.write(f.toString());
			// writer.write("\n");
			EnronToken current = null;
			EnronToken previous = null;
			String lexeme = null;
			boolean lastLineWasAHeader = false;
			while (recognizer.hasNext()) {

				previous = current;
				current = recognizer.next();
				lexeme = recognizer.getLexeme();

				// remove header
				if (current == HEADER) {
					while (current != LINESEPARATOR && recognizer.hasNext()) {
						current = recognizer.next();
					}
					if (current == LINESEPARATOR) {
						previous = current;
						current = recognizer.next();
					}
					lastLineWasAHeader = true;
				}
				if (lastLineWasAHeader == true && current == WS) {
					while (current != LINESEPARATOR && recognizer.hasNext()) {
						current = recognizer.next();
					}
					if (current == LINESEPARATOR) {
						previous = current;
						current = recognizer.next();
					}
				}
				// remove lines that start with hyphen
				if (current == HYPHEN && previous == LINESEPARATOR) {
					while (current != LINESEPARATOR && recognizer.hasNext()) {
						current = recognizer.next();
					}
				}

				if (current == STRING) {
					writer.write(lexeme);
				}
				if (current == LINESEPARATOR) {
					writer.write(" ");
					lastLineWasAHeader = false;
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
					while (recognizer.hasNext() && current != CLOSEDBRACES) {
						current = recognizer.next();
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
