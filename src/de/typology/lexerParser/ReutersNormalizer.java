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
public class ReutersNormalizer {

	private BufferedReader reader;
	private BufferedWriter writer;
	private String line;
	private double stringCount;
	private double numberCount;
	private double numberStringProportion;

	public ReutersNormalizer(String input, String output) {
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
				this.line = this.line.replaceAll("-+", "-");

				String[] strings = this.line.split("\\s");
				this.stringCount = 0;
				this.numberCount = 0;

				for (String string : strings) {
					if (string.matches(".*[0-9].*")) {
						this.numberCount++;
					}
					if (string.matches(".*[a-z]+.*")) {
						this.stringCount++;
					}
				}
				this.numberStringProportion = this.numberCount
						/ (strings.length + 1);
				// stringCount + 1 to prevent division by zero

				if (this.numberStringProportion < 0.2 && this.stringCount > 0) {
					// print strings if less then 25% are numbers and contains
					// at least one string
					for (String s : strings) {
						if (!s.isEmpty()) {
							this.writer.write(s + " ");
						}
					}
					this.writer.write("\n");
				}
			}
			this.writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
