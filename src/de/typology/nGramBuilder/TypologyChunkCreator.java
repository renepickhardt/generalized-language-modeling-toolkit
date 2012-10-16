package de.typology.nGramBuilder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;

import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public class TypologyChunkCreator extends ChunkCreator {

	public TypologyChunkCreator() {
		super();
	}

	public TypologyChunkCreator(String[] mostFrequentLetters) {
		super(mostFrequentLetters);
	}

	public void createNGramChunks(String fromFile) {
		String[] mostFrequentLetters = this.getMostFrequentStartingLetters();

		BufferedReader br = IOHelper.openReadFile(fromFile, 8 * 1024 * 1024);
		String line = "";
		int cnt = 0;

		HashMap<String, BufferedWriter> writers = IOHelper.createWriter(
				Config.get().nGramsNotAggregatedPath, mostFrequentLetters);

		try {
			while ((line = br.readLine()) != null) {
				cnt++;
				String[] tokens = line.split(" ");
				for (int i = Config.get().nGramLength; i < tokens.length; i++) {
					boolean first = true;
					BufferedWriter bw = null;
					try {

						for (int j = i - Config.get().nGramLength; j < i; j++) {
							if (first) {
								String token = tokens[i
										- Config.get().nGramLength];
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
							if (j < i - 1) {
								bw.write("\t");
							}
						}
					} catch (IndexOutOfBoundsException e) {
						continue;
					}
					bw.write("\t#1\n");
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
