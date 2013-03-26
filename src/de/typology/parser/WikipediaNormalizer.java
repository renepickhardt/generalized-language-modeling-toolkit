package de.typology.parser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

import de.typology.utils.IOHelper;

/**
 * @author Martin Koerner
 * 
 *         derived from
 *         http://101companies.org/index.php/101implementation:javaLexer
 * 
 */
public class WikipediaNormalizer {

	private BufferedReader reader;
	private BufferedWriter writer;

	public WikipediaNormalizer(String input, String output) {
		this.reader = IOHelper.openReadFile(input);
		this.writer = IOHelper.openWriteFile(output, 32 * 1024 * 1024);
	}

	public void normalize() {
		String line;
		try {
			while ((line = this.reader.readLine()) != null) {
				if (line.contains("<DISAMBIGUATION>") || line.contains("<TOC>")
						|| line.contains("<SYNTAXERROR>")
						|| line.contains("NOTOC")) {
					line = "";
				}
				line = line.replaceAll(" +", " ");
				if (line.startsWith(" ")) {
					line = line.substring(1, line.length());
				}
				line = line.replaceAll(" ,", ",");
				line = line.replaceAll(" ;", ";");
				line = line.replaceAll(" \\.", ".");
				line = line.replaceAll(" !", "!");
				line = line.replaceAll(" \\?", "\\?");
				line = line.replaceAll(",+", ",");
				line = line.replaceAll(";+", ";");
				line = line.replaceAll("\\.+", ".");
				line = line.replaceAll("!+", "!");
				line = line.replaceAll("\\?+", "\\?");

				line = line.replaceAll("''+", "");

				line = line.replaceAll(" +", " ");

				// remove some unwanted signs
				line = line.replaceAll("\\?", "");
				line = line.replaceAll("-", "");

				if (!line.isEmpty()) {
					this.writer.write(line);
					this.writer.write('\n');
					this.writer.flush();
				}
			}
			this.writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
