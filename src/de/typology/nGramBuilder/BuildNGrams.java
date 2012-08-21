package de.typology.nGramBuilder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;

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
		int cnt = 0;
		try {
			while ((line = br.readLine()) != null) {
				cnt++;
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
				if (cnt > 10) {
					break;
				}
			}
			bw.close();
			br.close();

			Process p = Runtime
					.getRuntime()
					.exec("sort --output=/var/lib/datasets/test /var/lib/datasets/parsedNGrams.txt");
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(
					p.getInputStream()));
			String s = "";
			while ((s = stdInput.readLine()) != null) {
				System.out.println(s);
			}
			System.out.println("sorting done need to aggregate now");

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
