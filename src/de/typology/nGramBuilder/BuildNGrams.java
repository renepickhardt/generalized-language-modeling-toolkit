package de.typology.nGramBuilder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

import de.typology.utils.Config;
import de.typology.utils.IOHelper;
import de.typology.utils.SystemHelper;

public class BuildNGrams {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		// read from larg text corpus and save ngrams on the fly
		BufferedReader br = IOHelper
				.openReadFile(Config.get().parsedWikiOutputPath);
		BufferedWriter bw = IOHelper.openWriteFile(Config.get().parsedNGrams);
		String line = "";
		long cnt = 0;
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
				if (cnt % 1000 == 0) {
					System.out.println("processed articles:" + cnt);
					if (cnt > 300000) {
						break;
					}
				}

			}
			bw.close();
			br.close();

			System.out.println("start sorting");

			SystemHelper.runUnixCommand("sort --output="
					+ Config.get().sortedNGrams + " "
					+ Config.get().parsedNGrams);

			System.out.println("sorting done need to aggregate now");

			SystemHelper.runUnixCommand("rm -rf " + Config.get().parsedNGrams);

			br = IOHelper.openReadFile(Config.get().sortedNGrams);
			bw = IOHelper.openWriteFile(Config.get().parsedNGrams);
			String currentLine = br.readLine();
			cnt = 0;
			while (currentLine != null) {
				String nextLine = br.readLine();
				int cooccurence = 1;
				while (currentLine.equals(nextLine)) {
					cooccurence++;
					nextLine = br.readLine();
				}
				bw.write("#" + cooccurence + "\t" + currentLine + "\n");
				if (cnt++ % 1000 == 0) {
					System.out.println("processed ngrams: " + cnt);
					bw.flush();
				}
				currentLine = nextLine;
			}
			bw.flush();
			bw.close();
			br.close();

			SystemHelper.runUnixCommand("rm -rf " + Config.get().sortedNGrams);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
