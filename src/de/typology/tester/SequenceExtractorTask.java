package de.typology.tester;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import org.apache.commons.io.FileUtils;

import de.typology.patterns.PatternTransformer;

/**
 * This class takes an ArrayList of sequences and a directory of Files as an
 * input and writes all occurrences of the sequences into new files in the
 * outputDirectory
 * 
 * @author Martin Koerner
 * 
 */
public class SequenceExtractorTask implements Runnable {

	private ArrayList<String> originalSequences;
	private boolean[] pattern;
	private File inputDirectory;
	private File outputDirectory;
	private String delimiter;

	private HashSet<String> newSequences;

	public SequenceExtractorTask(ArrayList<String> originalSequences,
			boolean[] originalPattern, boolean[] pattern, File inputDirectory,
			File outputDirectory, String delimiter) {
		this.originalSequences = originalSequences;
		this.pattern = pattern;

		this.inputDirectory = inputDirectory;
		this.outputDirectory = outputDirectory;
		if (outputDirectory.exists()) {
			try {
				FileUtils.deleteDirectory(outputDirectory);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		outputDirectory.mkdirs();
		this.delimiter = delimiter;

	}

	@Override
	public void run() {
		System.out.println(this.inputDirectory + "; "
				+ PatternTransformer.getStringPattern(this.pattern));
		this.buildNewSequences();

		for (File inputFile : this.inputDirectory.listFiles()) {
			File outputFile = new File(this.outputDirectory.getAbsolutePath()
					+ "/" + inputFile.getName());
			if (inputFile.getName().equals("all")) {
				try {
					FileUtils.copyFile(inputFile, outputFile);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			try {
				BufferedReader inputFileReader = new BufferedReader(
						new FileReader(inputFile));
				BufferedWriter outputFileWriter = new BufferedWriter(
						new FileWriter(outputFile));
				String line;
				while ((line = inputFileReader.readLine()) != null) {
					if (this.newSequences
							.contains(line.split(this.delimiter)[0])) {
						outputFileWriter.write(line);
					}
				}
				inputFileReader.close();
				outputFileWriter.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}

	private void buildNewSequences() {
		this.newSequences = new HashSet<String>();

		for (String originalLine : this.originalSequences) {
			String[] originalLineSplit = originalLine.split("\\s");
			int linePointer = 0;
			while (originalLineSplit.length - linePointer >= this.pattern.length) {
				// build current Sequence
				String currentSequence = "";
				for (int i = 0; i < this.pattern.length; i++) {
					currentSequence += originalLineSplit[linePointer + i] + " ";
				}
				currentSequence = currentSequence.replaceFirst(" $", "");

				String[] currentSequenceSplit = currentSequence.split("\\s");
				String newSequence = "";
				for (int i = 0; i < this.pattern.length; i++) {
					if (this.pattern[i]) {
						if (this.pattern[i]) {
							newSequence += currentSequenceSplit[i] + " ";
						}
					}
				}
				newSequence = newSequence.replaceFirst(" $", "");
				if (newSequence.length() > 0) {
					this.newSequences.add(newSequence);
				}

				linePointer++;
			}
		}
		for (String s : this.newSequences) {
			System.out.println(s);
		}
	}
}
