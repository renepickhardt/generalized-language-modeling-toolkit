package de.typology.splitter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * A class for modifying the sequences in InputDirectory based on the given
 * Pattern. The modified sequences are returned as outputStream
 * 
 * @author Martin Koerner
 * 
 */
public class SequenceModifier implements Runnable {
	private File inputDirectory;
	private OutputStream outputStream;
	private String delimiter;
	private boolean[] pattern;

	public SequenceModifier(File inputDirectory, OutputStream outputStream,
			String delimiter, boolean[] pattern) {
		this.inputDirectory = inputDirectory;
		this.outputStream = outputStream;
		this.delimiter = delimiter;
		this.pattern = pattern;
	}

	@Override
	public void run() {

		BufferedWriter outputStreamWriter = new BufferedWriter(
				new OutputStreamWriter(this.outputStream));
		try {
			for (File inputFile : this.inputDirectory.listFiles()) {
				BufferedReader inputFileReader = new BufferedReader(
						new FileReader(inputFile));
				String line;
				while ((line = inputFileReader.readLine()) != null) {
					String[] words = line.split(this.delimiter)[0].split("\\s");
					String modifiedWords = "";
					try {
						for (int i = 0; i < this.pattern.length; i++) {
							if (this.pattern[i]) {
								modifiedWords += words[i] + " ";
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					modifiedWords = modifiedWords.replaceFirst(" $", "");
					// TODO: better solution?
					if (words[0].equals("<fs>")) {
						// for kneser-ney smoothing: every sequence that starts
						// with <fs> counts as a new sequence
						if (!this.pattern[0]) {
							for (int i = 0; i < Integer.parseInt(line
									.split(this.delimiter)[1]); i++) {
								outputStreamWriter.write(modifiedWords + "\n");
							}
						}
						// if pattern[0]==true: leave out sequence
					} else {
						outputStreamWriter.write(modifiedWords + "\n");
					}
				}
				inputFileReader.close();
			}
			outputStreamWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
