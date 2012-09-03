package de.typology.lexerParser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;

public class DGTTMNormalizer {

	private BufferedReader reader;
	private BufferedWriter writer;
	HashSet<String> keywords;

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
		String line;
		try {
			while ((line = this.reader.readLine()) != null) {
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
