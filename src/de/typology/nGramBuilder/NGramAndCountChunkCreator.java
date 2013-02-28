package de.typology.nGramBuilder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;

import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public class NGramAndCountChunkCreator extends ChunkCreator {
	NGramAndCountChunkCreator() {
		super();
	}

	NGramAndCountChunkCreator(String[] mostFrequentLetters) {
		super(mostFrequentLetters);
	}

	public void createNGramChunks(String fromFile, int n, String trainingPath) {
		if (n > Config.get().nGramLength) {
			IOHelper.strongLog("can't create " + n
					+ "-grams only allowed to build up to "
					+ Config.get().nGramLength + "-grams.");
			return;
		}

		String extension = "." + n + "gc";

		String[] mostFrequentLetters = this.getMostFrequentStartingLetters();

		BufferedReader br = IOHelper.openReadFile(fromFile, 8 * 1024 * 1024);
		String line = "";
		int cnt = 0;

		HashMap<String, BufferedWriter> writers = IOHelper.createWriter(
				trainingPath + Config.get().nGramsNotAggregatedPath + "/" + n,
				mostFrequentLetters, extension);

		try {
			while ((line = br.readLine()) != null) {
				cnt++;
				String[] tokens = line.split("\t");
				for (int i = n; i < tokens.length; i++) {
					boolean first = true;
					BufferedWriter bw = null;
					try {
						for (int j = i - n; j <= i; j++) {
							if (first) {
								String token = tokens[j];
								String key = null;
								key = token.substring(0, 1);
								bw = writers.get(key);
								if (bw == null) {
									key = "other";
									bw = writers.get(key);
								}
								first = false;
							}
							bw.write(tokens[j]);
							if (j < i) {
								bw.write("\t");
							}
						}
					} catch (IndexOutOfBoundsException e) {
						continue;
					}
					bw.write("\n");
				}
				if (cnt % 50000 == 0) {
					IOHelper.log("processed " + cnt + " articles into chunks:");
				}
			}
			for (String k : writers.keySet()) {
				writers.get(k).close();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
