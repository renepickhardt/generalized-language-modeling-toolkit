package de.typology.lexerParser;

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
public class DGTTMNormalizer {

	private BufferedReader reader;
	private BufferedWriter writer;
	private String line;

	public DGTTMNormalizer(String input, String output) {
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
				this.line = this.line.replaceAll(" +", " ");
				if (this.line.startsWith(" ")) {
					this.line = this.line.substring(1, this.line.length());
				}
				this.line = this.line.replaceAll(" ,", ",");
				this.line = this.line.replaceAll(" ;", ";");
				this.line = this.line.replaceAll(" \\.", ".");
				this.line = this.line.replaceAll(" !", "!");
				this.line = this.line.replaceAll(" \\?", "\\?");
				this.line = this.line.replaceAll(",+", ",");
				this.line = this.line.replaceAll(";+", ";");
				this.line = this.line.replaceAll("\\.+", ".");
				this.line = this.line.replaceAll("!+", "!");
				this.line = this.line.replaceAll("\\?+", "\\?");

				if (!this.line.isEmpty()) {
					this.writer.write(this.line);
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
