package de.typology.tester;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.typology.indexes.WordIndex;
import de.typology.patterns.PatternBuilder;
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

	Logger logger = LogManager.getLogger(this.getClass().getName());

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
		outputDirectory.mkdir();
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

		this.logger.info("build continuation for: "
				+ currentInputDirectory.getAbsolutePath());

		// initialize continuationMap and build continuation labels
		HashMap<boolean[], HashMap<String, Long>> continuationMap = new HashMap<boolean[], HashMap<String, Long>>();
		ArrayList<boolean[]> allPatterns = PatternBuilder
				.getGLMPatterns(this.originalPattern.length);
		for (boolean[] pattern : allPatterns) {
			if (pattern.length == this.originalPattern.length
					&& PatternTransformer.getStringPattern(pattern).contains(
							"0")) {
				// set a new HashMap
				continuationMap.put(pattern, new HashMap<String, Long>());
			}
		}
		// replace the first word with _
		boolean[] replaceFirstWordPattern = this.originalPattern.clone();
		replaceFirstWordPattern[0] = false;

		// set a new HashMap

		if (replaceFirstWordPattern.length > 1) {
			continuationMap.put(replaceFirstWordPattern,
					new HashMap<String, Long>());
		}

		if (this.originalPattern.length > 1) {

			// add continuationPatterns with replaced last word
			ArrayList<boolean[]> continuationWithoutLastPatterns = new ArrayList<boolean[]>();
			for (Entry<boolean[], HashMap<String, Long>> continuationMapEntry : continuationMap
					.entrySet()) {
				boolean[] continuationPatternWithoutLast = continuationMapEntry
						.getKey().clone();
				continuationPatternWithoutLast[continuationPatternWithoutLast.length - 1] = false;
				continuationWithoutLastPatterns
						.add(continuationPatternWithoutLast);
			}
			for (boolean[] continuationPatternWithoutLast : continuationWithoutLastPatterns) {
				if (PatternTransformer.getStringPattern(
						continuationPatternWithoutLast).contains("1")) {
					continuationMap.put(continuationPatternWithoutLast,
							new HashMap<String, Long>());
				}

			}

			// replace the last word with _
			boolean[] replaceLastWordPattern = this.originalPattern.clone();
			replaceLastWordPattern[replaceLastWordPattern.length - 1] = false;

			// set a new HashMap
			continuationMap.put(replaceLastWordPattern,
					new HashMap<String, Long>());

		}

		// add sequences to continuationMap
		this.logger.info("add testing sequences to continuationMap for "
				+ PatternTransformer.getStringPattern(this.originalPattern));
		try {
			for (File originalSequencesFile : this.originalSequencesDirectory
					.listFiles()) {
				BufferedReader originalSequencesReader = new BufferedReader(
						new FileReader(originalSequencesFile));
				String line;
				while ((line = originalSequencesReader.readLine()) != null) {
					for (Entry<boolean[], HashMap<String, Long>> continuationMapEntry : continuationMap
							.entrySet()) {
						continuationMapEntry.getValue().put(
								SequenceFormatter.removeWords(
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

		// count number of different sequences
		long lineCount = 0L;
		for (File inputFile : currentInputDirectory.listFiles()) {
			try {
				BufferedReader inputFileReader = new BufferedReader(
						new FileReader(inputFile));
				String line;
				while ((line = inputFileReader.readLine()) != null) {
					if (!line.startsWith("<fs>")) {
						lineCount++;
					}
				}
				inputFileReader.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// write result to continuation files without ones

		if (this.originalPattern.length < 3) {
			File currentOutputDirectory;
			if (this.originalPattern.length == 1) {
				currentOutputDirectory = new File(
						this.outputDirectory.getAbsolutePath() + "/_");
			} else {
				currentOutputDirectory = new File(
						this.outputDirectory.getAbsolutePath() + "/__");
			}
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
				currentOutputWriter.write(String.valueOf(lineCount) + "\n");
				currentOutputWriter.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		// else scan files in inputDirectory for matching sequences
		this.logger.info("scan files for "
				+ PatternTransformer.getStringPattern(this.originalPattern));
		for (File inputFile : currentInputDirectory.listFiles()) {
			try {
				BufferedReader inputFileReader = new BufferedReader(
						new FileReader(inputFile));
				String line;
				while ((line = inputFileReader.readLine()) != null) {
					String[] lineSplit = line.split(this.delimiter);

					// go over different continuation patterns
					for (Entry<boolean[], HashMap<String, Long>> continuationMapEntry : continuationMap
							.entrySet()) {

						// some logic for <fs>
						if (lineSplit[0].startsWith("<fs>")) {
							if (continuationMapEntry.getKey()[0]) {
								continue;
							} else {
								if (continuationMapEntry.getKey().length == 2
										&& lineSplit[0].contains("<s>")) {
									// TODO write "<s> 0" ?
								} else {
									String continuationSequence = SequenceFormatter
											.removeWords(lineSplit[0],
													continuationMapEntry
															.getKey());

									if (continuationMapEntry.getValue()
											.containsKey(continuationSequence)) {
										continuationMapEntry
												.getValue()
												.put(continuationSequence,
														continuationMapEntry
																.getValue()
																.get(continuationSequence)
																+ Long.parseLong(lineSplit[1]));
									}
								}
							}
						} else {
							String continuationSequence = SequenceFormatter
									.removeWords(lineSplit[0],
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
				}
				inputFileReader.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		// write result
		this.logger.info("write results for "
				+ PatternTransformer.getStringPattern(this.originalPattern));
		for (Entry<boolean[], HashMap<String, Long>> continuationMapEntry : continuationMap
				.entrySet()) {
			File currentOutputDirectory = new File(
					this.outputDirectory.getAbsolutePath()
							+ "/"
							+ PatternTransformer.getStringPattern(
									continuationMapEntry.getKey()).replaceAll(
									"0", "_"));
			HashMap<Integer, BufferedWriter> continuationWriters = this.wordIndex
					.openWriters(currentOutputDirectory);
			for (Entry<String, Long> continuationMapEntryMapEntry : continuationMapEntry
					.getValue().entrySet()) {
				try {
					if (continuationMapEntryMapEntry.getValue() > 0) {
						continuationWriters.get(
								this.wordIndex
										.rank(continuationMapEntryMapEntry
												.getKey().split("\\s")[0]))
								.write(continuationMapEntryMapEntry.getKey()
										+ this.delimiter
										+ continuationMapEntryMapEntry
												.getValue() + "\n");
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
			this.wordIndex.closeWriters(continuationWriters);
		}

	}
}
