package de.typology.nGramBuilder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public class BuildNGrams {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		BufferedReader br = IOHelper
				.openReadFile(Config.get().parsedWikiOutputPath);
		BufferedWriter bw = IOHelper.openWriteFile(Config.get().parsedNGrams);
		String line = "";
		try {
			while ((line = br.readLine()) != null) {
				String[] tokens = line.split(" ");
				for (int i = Config.get().nGramLength; i < tokens.length; i++) {
					for (int j = i - Config.get().nGramLength; j < i; j++) {
						bw.write(tokens[j]);
						if (j < i - 1) {
							bw.write("\t");
						}
					}
					bw.write("\n");
				}
				bw.flush();
			}
			bw.close();
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
