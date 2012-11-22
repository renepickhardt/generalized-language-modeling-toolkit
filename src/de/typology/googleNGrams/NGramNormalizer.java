package de.typology.googleNGrams;

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
public class NGramNormalizer {

	private BufferedReader reader;
	private BufferedWriter writer;
	private String line;

	public NGramNormalizer(String input, String output) {
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
				this.line = this.line.replaceAll(" ¿", "¿");
				this.line = this.line.replaceAll(" ¡", "¡");

				this.line = this.line.replaceAll("\\.+", ".");
				this.line = this.line.replaceAll(",+", ",");
				this.line = this.line.replaceAll(";+", ";");
				this.line = this.line.replaceAll(":+", ":");
				// this.line = this.line.replaceAll("-+", "-");
				this.line = this.line.replaceAll("'+", "'");
				this.line = this.line.replaceAll("!+", "!");
				this.line = this.line.replaceAll("\\?+", "\\?");
				this.line = this.line.replaceAll("¿+", "¿");
				this.line = this.line.replaceAll("¡+", "¡");
				while (true) {
					if (this.line.startsWith(".")
							|| this.line.startsWith(",")
							|| this.line.startsWith(";")
							|| this.line.startsWith(":")
							// || this.line.startsWith("-")
							|| this.line.startsWith("'")
							|| this.line.startsWith("?")
							|| this.line.startsWith("!")
							|| this.line.startsWith("¿")
							|| this.line.startsWith("¡")) {
						this.line = this.line.substring(1, this.line.length());
					} else {
						break;
					}
				}
				if (this.line.startsWith(" ")) {
					this.line = this.line.substring(1, this.line.length());
				}

				// change format from w1 ... w3\tcount\n to w1\t...w3\t#count\n
				String[] splitLine = this.line.split("\\s");
				if (splitLine.length > 2) {
					for (String word : Arrays.copyOfRange(splitLine, 0,
							splitLine.length - 1)) {
						this.writer.write(word + "\t");
					}
					this.writer.write("#");
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
