package de.typology.parser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Locale;

import de.typology.utils.IOHelper;

/**
 * @author Martin Koerner
 * 
 *         derived from
 *         http://101companies.org/index.php/101implementation:javaLexer
 * 
 */
public class ReutersNormalizer extends Normalizer {

	private BufferedReader reader;
	private BufferedWriter writer;
	private String line;
	private double stringCount;
	private double numberCount;
	private double numberStringProportion;

	public ReutersNormalizer(String input, String output, Locale locale) {
		super(locale);
		this.reader = IOHelper.openReadFile(input);
		this.writer = IOHelper.openWriteFile(output, 32 * 1024 * 1024);
	}

	public void normalize() {

		try {
			while ((this.line = this.reader.readLine()) != null) {
				this.line = this.normalizeString(this.line);
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
						/ (this.numberCount + this.stringCount);
				// stringCount + 1 to prevent division by zero

				if (this.numberStringProportion < 0.2 && this.stringCount > 1) {
					// print strings if less then 25% are numbers and contains
					// at least one string
					this.writer.write(this.line);
					this.writer.flush();
				}
			}
			this.writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
