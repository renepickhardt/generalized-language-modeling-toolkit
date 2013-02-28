package de.typology.nGramBuilder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;

import de.typology.utils.IOHelper;

public class TypologyAndCountChunkCreator extends ChunkCreator {

	public TypologyAndCountChunkCreator() {
		super();
	}

	public TypologyAndCountChunkCreator(String[] mostFrequentLetters) {
		super(mostFrequentLetters);
	}

	public void createTypoelogyEdgeChunks(String fromFile, String outPath,
			int type) {
		String[] mostFrequentLetters = this.getMostFrequentStartingLetters();

		BufferedReader br = IOHelper.openReadFile(fromFile, 8 * 1024 * 1024);
		String line = "";
		int cnt = 0;

		String extension = "." + type + "ec";

		HashMap<String, BufferedWriter> writers = IOHelper.createWriter(outPath
				+ "/" + type, mostFrequentLetters, extension);

		try {
			while ((line = br.readLine()) != null) {
				cnt++;
				String[] tokens = line.split("\t");
				BufferedWriter bw = null;
				String first = tokens[0];
				String second = tokens[tokens.length-2];
				String count=tokens[tokens.length-1];
				try {
					String key = null;
					key = first.substring(0, 1);
					bw = writers.get(key);
					if (bw == null) {
						key = "other";
						bw = writers.get(key);
					}
				} catch (IndexOutOfBoundsException e) {
					continue;
				}
				bw.write(first + "\t" + second + "\t"+count+"\n");

				if (cnt % 50000 == 0) {
					IOHelper.log("processed " + cnt
							+ " articles into typology edge chunks of type: "
							+ type);
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
