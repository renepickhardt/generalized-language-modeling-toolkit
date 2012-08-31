package de.typology.lexerParser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ReutersNormalizer {

	private BufferedReader reader;
	private BufferedWriter writer;

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
		String line;
		try {
			while ((line = this.reader.readLine()) != null) {
				line = line.replaceAll(" +", " ");
				if (line.startsWith(" ")) {
					line = line.substring(1, line.length());
				}
				line = line.replaceAll(" ,", ",");
				line = line.replaceAll(" \\p{Punct}", ".");
				line = line.replaceAll(",,+", ",");
				line = line.replaceAll("\\p{Punct}+\\p{Punct}+", ".");

				String[] strings = line.split("\\s");
				for (String s : strings) {
					if (s.contains("@") || s.contains("http")) {
						// do nothing
					} else {
						this.writer.write(s + " ");
					}
				}
				this.writer.write('\n');
				this.writer.flush();
			}
			this.writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
