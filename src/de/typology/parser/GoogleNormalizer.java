package de.typology.parser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;

import de.typology.utils.IOHelper;

/**
 * Normalizes the format of a given (and parsed) google ngrams file, removing
 * multiple white spaces and punctuation marks and white spaces before
 * punctuation marks.
 * <p>
 * derived from http://101companies.org/index.php/101implementation:javaLexer
 * 
 * @author Martin Koerner
 */
public class GoogleNormalizer {

	private BufferedReader reader;
	private BufferedWriter writer;
	private String line;
	private int nGramCount;

	public GoogleNormalizer(String input, String output, int nGramCount) {
		this.nGramCount = nGramCount;
		this.reader = IOHelper.openReadFile(input);
		this.writer = IOHelper.openWriteFile(output, 32 * 1024 * 1024);
	}

	public void normalize() {
		try {
			while ((this.line = this.reader.readLine()) != null) {
				if (this.line.contains("-")) {
					this.line = "";
				}
				this.line = this.line.replaceAll(" +", " ");
				this.line = this.line.replaceAll(" \\.", ".");
				this.line = this.line.replaceAll(" ,", ",");
				this.line = this.line.replaceAll(" ;", ";");
				this.line = this.line.replaceAll(" :", ":");
				// this.line = this.line.replaceAll("\t-", "-");
				this.line = this.line.replaceAll(" '", "'");
				this.line = this.line.replaceAll(" \\?", "\\?");
				this.line = this.line.replaceAll(" !", "!");

				this.line = this.line.replaceAll("\\.+", ".");
				this.line = this.line.replaceAll(",+", ",");
				this.line = this.line.replaceAll(";+", ";");
				this.line = this.line.replaceAll(":+", ":");
				// this.line = this.line.replaceAll("-+", "-");
				this.line = this.line.replaceAll("'+", "'");
				this.line = this.line.replaceAll("!+", "!");
				this.line = this.line.replaceAll("\\?+", "\\?");
				while (true) {
					if (this.line.startsWith(".")
							|| this.line.startsWith(",")
							|| this.line.startsWith(";")
							|| this.line.startsWith(":")
							// || this.line.startsWith("-")
							|| this.line.startsWith("'")
							|| this.line.startsWith("?")
							|| this.line.startsWith("!")) {
						this.line = this.line.substring(1, this.line.length());
					} else {
						break;
					}
				}
				if (this.line.startsWith(" ")) {
					this.line = this.line.substring(1, this.line.length());
				}

				// remove some unwanted signs
				this.line = this.line.replaceAll("\\?", "");
				this.line = this.line.replaceAll("-", "");

				String[] splitLine = this.line.split("\\s");
				if (splitLine.length == this.nGramCount + 1) {
					for (String word : Arrays.copyOfRange(splitLine, 0,
							splitLine.length - 1)) {
						this.writer.write(word + "\t");
					}
					this.writer.write(splitLine[splitLine.length - 1]);
					this.writer.write("\n");
					this.writer.flush();
				}
			}
			this.writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
