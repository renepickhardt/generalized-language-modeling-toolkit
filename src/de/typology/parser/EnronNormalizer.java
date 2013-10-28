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
public class EnronNormalizer extends Normalizer {

	private BufferedReader reader;
	private BufferedWriter writer;
	private String line;
	private String originalLine;
	private double stringCount;
	private double atCount;
	private double numberCount;
	double numberStringProportion;
	double atStringProportion;

	public EnronNormalizer(String input, String output, Locale locale) {
		super(locale);
		this.reader = IOHelper.openReadFile(input);
		this.writer = IOHelper.openWriteFile(output, 32 * 1024 * 1024);
	}

	public void normalize() {

		try {
			while ((this.line = this.reader.readLine()) != null) {
				this.originalLine = this.line;

				this.line = this.normalizeString(this.line);

				String[] strings = this.line.split("\\s");
				this.stringCount = 0;
				this.atCount = 0;
				this.numberCount = 0;
				if (!this.originalLine.startsWith(" - ")) {
					// attachments start with " - "
					for (int i = 0; i < strings.length; i++) {
						if (strings[i].contains("@")) {
							this.atCount++;
						}
						if (strings[i].contains("http")
								|| strings[i].contains("@")
								|| strings[i].contains("--")
								|| strings[i].contains("/")
								|| strings[i].contains("Http")
								|| strings[i].contains("www")
								|| strings[i].contains(".com")
								|| strings[i].contains("=")) {
							strings[i] = "";
						}
						if (strings[i].contains("<ENDOFMAIL>")) {
							// remove token
							strings[i] = "";
						}
						if (strings[i].matches(".*[0-9].*")) {
							this.numberCount++;
						}
						if (strings[i].matches(".*[a-z]+.*")) {
							this.stringCount++;
						}
					}
					this.numberStringProportion = this.numberCount
							/ (strings.length + 1);
					this.atStringProportion = this.atCount
							/ (strings.length + 1);
					// + 1 to prevent division by zero

					if (this.numberStringProportion < 0.3
							&& this.atStringProportion < 0.15
							&& this.stringCount > 0) {
						// print strings if less then 25% are numbers, less
						// than 20% contain @ and contains at least one string
						for (String s : strings) {
							if (!s.isEmpty()) {
								this.writer.write(s + " ");

							}
						}
					}
					if (this.originalLine.equals("<ENDOFMAIL>")) {
						this.writer.flush();
					}
				}
			}
			this.writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
