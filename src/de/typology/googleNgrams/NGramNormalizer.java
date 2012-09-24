package de.typology.googleNgrams;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import de.typology.utils.Config;

/**
 * @author Martin Koerner
 * 
 *         derived from
 *         http://101companies.org/index.php/101implementation:javaLexer
 * 
 */
public class NGramNormalizer {
	public static void main(String[] args) throws IOException {
		NGramNormalizer ngn = new NGramNormalizer(
				Config.get().googleNgramsPath,
				Config.get().googleNgramsNormalizedPath);
		System.out.println("start cleanup");
		ngn.normalize();
		System.out.println("cleanup done");
		System.out.println("generate indicator file");
		File done = new File(Config.get().normalizedWikiOutputPath + "IsDone");
		done.createNewFile();
		System.out.println("done");
	}

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
				System.out.println(this.line + " --> ");
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

				String[] splitLine = this.line.split("\\s");
				if (splitLine[0] == "." || splitLine[0] == ","
						|| splitLine[0] == "") {
					if (!this.line.isEmpty()) {
						this.writer.write(this.line);
						this.writer.write('\n');
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
