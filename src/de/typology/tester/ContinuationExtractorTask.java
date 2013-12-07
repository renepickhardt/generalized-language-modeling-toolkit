package de.typology.tester;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;

import de.typology.indexes.WordIndex;
import de.typology.patterns.PatternTransformer;
import de.typology.utils.SequenceFormatter;

/**
 * This class calculates the continuation counts for a given list of sequences
 * by scanning existing absolute counts.
 * 
 * @author Martin Koerner
 * 
 */
public class ContinuationExtractorTask implements Runnable {

	private File originalSequencesDirectory;
	private boolean[] originalPattern;
	private File inputDirectory;
	private File outputDirectory;
	private WordIndex wordIndex;
	private String delimiter;

	public ContinuationExtractorTask(File originalSequencesDirectory,
			boolean[] originalPattern, File inputDirectory,
			File outputDirectory, WordIndex wordIndex, String delimiter) {
		this.originalSequencesDirectory = originalSequencesDirectory;
		this.originalPattern = originalPattern;
		this.inputDirectory = inputDirectory;
		this.outputDirectory = outputDirectory;
		this.wordIndex = wordIndex;
		this.delimiter = delimiter;
	}

	@Override
	public void run() {
		File currentInputDirectory = new File(
				this.inputDirectory.getAbsolutePath()
						+ "/"
						+ PatternTransformer
								.getStringPattern(this.originalPattern));
		HashMap<Integer, HashMap<String, Long>> continuationMap = new HashMap<Integer, HashMap<String, Long>>();
		HashMap<Integer, String> continuationLabelMap = new HashMap<Integer, String>();
		String originalStringPattern = PatternTransformer
				.getStringPattern(this.originalPattern);

		// initialize continuationMap and build continuation labels
		for (int i = 0; i < this.originalPattern.length; i++) {
			if (this.originalPattern[i]) {
				// set a new HashMap
				continuationMap.put(i, new HashMap<String, Long>());

				// build continuation label
				StringBuilder continuationStringBuilder = new StringBuilder(
						originalStringPattern);
				continuationStringBuilder.setCharAt(i, '_');
				continuationLabelMap.put(i,
						continuationStringBuilder.toString());
			}
		}

		// add sequences to continuationMap
		try {
			for (File originalSequencesFile : this.originalSequencesDirectory
					.listFiles()) {
				BufferedReader originalSequencesReader = new BufferedReader(
						new FileReader(originalSequencesFile));
				String line;
				while ((line = originalSequencesReader.readLine()) != null) {
					for (Entry<Integer, HashMap<String, Long>> continuationMapEntry : continuationMap
							.entrySet()) {
						continuationMapEntry.getValue().put(
								SequenceFormatter.removeWord(
										line.split(this.delimiter)[0],
										continuationMapEntry.getKey()), 0L);
					}
				}
				originalSequencesReader.close();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (Integer.bitCount(PatternTransformer
				.getIntPattern(this.originalPattern)) == 1) {
			// count number of different sequences
			long lineCount = 0L;
			for (File inputFile : currentInputDirectory.listFiles()) {
				try {
					BufferedReader inputFileReader = new BufferedReader(
							new FileReader(inputFile));
					while (inputFileReader.readLine() != null) {
						lineCount++;
					}
					inputFileReader.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			// write result
			for (Entry<Integer, HashMap<String, Long>> continuationMapEntry : continuationMap
					.entrySet()) {
				File currentOutputDirectory = new File(
						this.outputDirectory.getAbsolutePath()
								+ "/"
								+ continuationLabelMap.get(continuationMapEntry
										.getKey()));
				if (currentOutputDirectory.exists()) {
					try {
						FileUtils.deleteDirectory(currentOutputDirectory);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				currentOutputDirectory.mkdir();
				File currentOutputFile = new File(
						currentOutputDirectory.getAbsolutePath() + "/all");
				try {
					BufferedWriter currentOutputWriter = new BufferedWriter(
							new FileWriter(currentOutputFile));
					currentOutputWriter.write(String.valueOf(lineCount));
					currentOutputWriter.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		} else {
			// else scan files in inputDirectory for matching sequences
			for (File inputFile : currentInputDirectory.listFiles()) {
				try {
					BufferedReader inputFileReader = new BufferedReader(
							new FileReader(inputFile));
					String line;
					while ((line = inputFileReader.readLine()) != null) {
						String[] lineSplit = line.split(this.delimiter);

						// go over different continuation patterns
						for (Entry<Integer, HashMap<String, Long>> continuationMapEntry : continuationMap
								.entrySet()) {
							String continuationSequence = SequenceFormatter
									.removeWord(lineSplit[0],
											continuationMapEntry.getKey());
							if (continuationMapEntry.getValue().containsKey(
									continuationSequence)) {
								continuationMapEntry.getValue().put(
										continuationSequence,
										continuationMapEntry.getValue().get(
												continuationSequence) + 1);
							}
						}
					}
					inputFileReader.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				// write result
				for (Entry<Integer, HashMap<String, Long>> continuationMapEntry : continuationMap
						.entrySet()) {
					File currentOutputDirectory = new File(
							this.outputDirectory.getAbsolutePath()
									+ "/"
									+ continuationLabelMap
											.get(continuationMapEntry.getKey()));
					HashMap<Integer, BufferedWriter> continuationWriters = this.wordIndex
							.openWriters(currentOutputDirectory);
					for (Entry<String, Long> continuationMapEntryMapEntry : continuationMapEntry
							.getValue().entrySet()) {
						try {
							continuationWriters.get(
									this.wordIndex
											.rank(continuationMapEntryMapEntry
													.getKey().split("\\s")[0]))
									.write(continuationMapEntryMapEntry
											.getKey()
											+ this.delimiter
											+ continuationMapEntryMapEntry
													.getValue());
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					}
					this.wordIndex.closeWriters(continuationWriters);
				}

			}

		}
	}
}
