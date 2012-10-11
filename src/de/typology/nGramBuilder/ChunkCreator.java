package de.typology.nGramBuilder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public class ChunkCreator {
	private String[] mostFrequentStartingLetters;

	public String[] getMostFrequentStartingLetters() {
		return this.mostFrequentStartingLetters;
	}

	public void setMostFrequentStartingLetters(
			String[] mostFrequentStartingLetters) {
		this.mostFrequentStartingLetters = mostFrequentStartingLetters;
	}

	ChunkCreator() {
		IOHelper.strongLog("need parameter for ChunkCreator");
		System.exit(1);
	}

	ChunkCreator(String[] mostFrequentStartingLetters) {
		this.setMostFrequentStartingLetters(mostFrequentStartingLetters);
	}

	public boolean createSecondLevelChunks(String sourcePath,
			String fileExtension) {

		File[] files = IOHelper.getAllFilesInDirWithExtension(sourcePath,
				fileExtension, this.getClass().getName()
						+ ".createSecondLevelChunks");

		for (File f : files) {
			if (f == null) {
				continue;
			}
			if (!f.exists()) {
				continue;
			}
			if (f.length() > Config.get().fileChunkThreashhold) {
				IOHelper.log("need to further split file: " + f.getName());
				// TODO: introduce multithreading again? Probably not a good
				// idea since the task here is very disk intensive
				this.CreateDetailedChunks(
						sourcePath + f.separator + f.getName(), fileExtension);
				f.delete();
			} else {
				IOHelper.log("file '" + f.getName()
						+ "' is ready to be aggregated");
			}
		}
		return true;
	}

	private boolean CreateDetailedChunks(String sourceFile, String fileExtension) {
		BufferedReader br = IOHelper.openReadFile(sourceFile);
		String line = "";
		int cnt = 0;

		HashMap<String, BufferedWriter> writers = IOHelper.createWriter(
				sourceFile, this.mostFrequentStartingLetters);

		try {
			while ((line = br.readLine()) != null) {
				cnt++;
				String[] tokens = line.split("\\s");
				if (tokens.length < 3) {
					continue;
				}
				String key = tokens[1].substring(0, 1);
				BufferedWriter bw = null;
				bw = writers.get(key);
				if (bw == null) {
					key = "other";
					bw = writers.get(key);
				}
				bw.write(line + "\n");
				if (cnt % 1000000 == 0) {
					IOHelper.log("processed ngrams into smaller chunks:" + cnt);
				}
			}
			for (String key : writers.keySet()) {
				writers.get(key).close();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}
}
