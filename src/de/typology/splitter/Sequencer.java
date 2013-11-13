package de.typology.splitter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.typology.indexes.WordIndex;

/**
 * A class for splitting a text file (via inputStream) into sequences that are
 * stored in different files based on the indexFile in outputDirectory.
 * 
 * @author Martin Koerner
 * 
 */
public class Sequencer implements Runnable {
	protected InputStream inputStream;
	protected File outputDirectory;
	protected WordIndex wordIndex;
	protected boolean[] pattern;

	static Logger logger = LogManager.getLogger(Sequencer.class.getName());

	public Sequencer(InputStream inputStream, File outputDirectory,
			WordIndex wordIndex, boolean[] pattern) {
		this.inputStream = inputStream;
		this.outputDirectory = outputDirectory;
		this.wordIndex = wordIndex;
		this.pattern = pattern;

	}

	@Override
	public void run() {
		HashMap<Integer, BufferedWriter> writers = this.openWriters();
		// TODO: bufferSize calculation
		BufferedReader bufferedReader = new BufferedReader(
				new InputStreamReader(this.inputStream));
		// BufferedReader bufferedReader = new BufferedReader(
		// new InputStreamReader(this.inputStream), 10 * 8 * 1024);
		String line;
		try {
			while ((line = bufferedReader.readLine()) != null) {
				String[] lineSplit = line.split("\\s");
				int linePointer = 0;
				while (lineSplit.length - linePointer >= this.pattern.length) {
					String sequence = "";
					for (int i = 0; i < this.pattern.length; i++) {
						if (this.pattern[i]) {
							sequence += lineSplit[linePointer + i] + " ";
						}
					}
					sequence = sequence.replaceFirst(" $", "");
					sequence += "\n";

					// write sequence
					writers.get(this.wordIndex.rank(sequence.split(" ")[0]))
							.write(sequence);

					linePointer++;
				}
			}
			bufferedReader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		this.closeWriters(writers);
	}

	private HashMap<Integer, BufferedWriter> openWriters() {
		HashMap<Integer, BufferedWriter> writers = new HashMap<Integer, BufferedWriter>();

		// Runtime runtime = Runtime.getRuntime();
		// int mb = 1024 * 1024;
		// logger.debug("totalMemory: " + runtime.totalMemory() / mb);
		// logger.debug("buffersize: " + bufferSize / mb);

		// System.out.println("");
		//
		// int mb = 1024 * 1024;
		//
		// // Getting the runtime reference from system
		// Runtime runtime = Runtime.getRuntime();
		//
		// System.out.println("##### Heap utilization statistics [MB] #####");
		//
		// // Print used memory
		// System.out.println("Used Memory:\t"
		// + (runtime.totalMemory() - runtime.freeMemory()) / mb);
		//
		// // Print free memory
		// System.out.println("Free Memory:\t" + runtime.freeMemory() / mb);
		//
		// // Print total available memory
		// System.out.println("Total Memory:\t" + runtime.totalMemory() / mb);
		//
		// // Print Maximum available memory
		// System.out.println("Max Memory:\t" + runtime.maxMemory() / mb);

		File currentOutputDirectory = new File(
				this.outputDirectory.getAbsolutePath());

		currentOutputDirectory.mkdir();

		// calculate buffer size for writers
		// TODO: bufferSize calculation
		for (int fileCount = 0; fileCount < this.wordIndex.getLength(); fileCount++) {
			try {
				writers.put(fileCount, new BufferedWriter(new FileWriter(
						currentOutputDirectory.getAbsolutePath() + "/"
								+ fileCount)));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return writers;
	}

	private void closeWriters(HashMap<Integer, BufferedWriter> writers) {
		for (Entry<Integer, BufferedWriter> entry : writers.entrySet()) {
			try {
				entry.getValue().close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public boolean[] getPattern() {
		return this.pattern;
	}
}
