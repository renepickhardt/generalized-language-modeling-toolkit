package de.typology.googleNgrams;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author Martin Koerner
 * 
 *         derived from
 *         http://101companies.org/index.php/101implementation:javaLexer
 * 
 */
public class NGramNormalizer {

	private BufferedReader reader;
	private BufferedWriter writer;
	private String line;

	public NGramNormalizer(String input, String output) {
		try {
			this.reader = new BufferedReader(new FileReader(input));
			this.writer = new BufferedWriter(new FileWriter(output));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void normalize() {
		try {
			while ((this.line = this.reader.readLine()) != null) {
				if (this.line.contains("-")) {
					this.line = "";
				}
				this.line = this.line.replaceAll("\t+", "\t");
				this.line = this.line.replaceAll("\t\\.", ".");
				this.line = this.line.replaceAll("\t,", ",");
				this.line = this.line.replaceAll("\t;", ";");
				this.line = this.line.replaceAll("\t:", ":");
				// this.line = this.line.replaceAll("\t-", "-");
				this.line = this.line.replaceAll("\t'", "'");
				this.line = this.line.replaceAll("\t\\?", "\\?");
				this.line = this.line.replaceAll("\t!", "!");
				this.line = this.line.replaceAll("\t¿", "¿");
				this.line = this.line.replaceAll("\t¡", "¡");

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
				if (this.line.startsWith("\t")) {
					this.line = this.line.substring(1, this.line.length());
				}

				String[] splitLine = this.line.split("\\s");
				if (splitLine.length > 2) {
					this.writer.write(this.line + "\n");
					this.writer.flush();
				}
			}
			this.writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
