package de.typology.splitter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.typology.utils.Config;

/**
 * This class splits and samples a given input file into trainings and test data
 * 
 * The threasholds can be configured in config.txt the relevant fields are
 * 
 * splitDataRatio
 * 
 * smpleRate
 * 
 * nGramLength
 * 
 * @author Rene Pickhardt, Martin Koerner
 * 
 */
public class DataSetSplitter {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String outputDirectory = Config.get().outputDirectory
				+ Config.get().inputDataSet;

		DataSetSplitter dss = new DataSetSplitter(new File(outputDirectory),
				"normalized.txt");
		dss.split("training.txt", "learning.txt", "testing.txt", 5);
		dss.splitIntoSequences(new File(outputDirectory + "/training.txt"),
				Config.get().modelLength, Config.get().numberOfQueries);

	}

	private File directory;

	private String inputName;

	Logger logger = LogManager.getLogger(this.getClass().getName());

	public DataSetSplitter(File directory, String inputName) {

		this.directory = directory;
		this.inputName = inputName;
	}

	/**
	 * Takes a given input file and provides a 3 way split. The file can be
	 * sampled via the sampleRatio. A high sample ratio means that a large
	 * portion of the file is being thrown away
	 * 
	 * There the splitDataRatio specifies the percentage of the file that is
	 * used as training data. The rest will be used as test and learing data.
	 * 
	 * The last parameter splitTestRatio is usually set to 50 and means that the
	 * test data is also devided again into learning and testing data
	 * 
	 * 
	 * @param inputFile
	 *            potentially large text file that needs to be split
	 * @param trainingFileName
	 *            filename where training data are to be stored
	 * @param learningFileName
	 *            filename where learning data are to be stored
	 * @param testingFileName
	 *            filename where test data are to be stored
	 */
	public void split(String trainingFileName, String learningFileName,
			String testingFileName, int sequenceLength) {
		this.logger.info("splitting into training, testing and learning file: "
				+ this.directory + "/" + this.inputName);
		try {
			BufferedReader reader = new BufferedReader(new FileReader(
					this.directory.getAbsolutePath() + "/" + this.inputName));
			BufferedWriter trainingDataWriter = new BufferedWriter(
					new FileWriter(this.directory.getAbsolutePath() + "/"
							+ trainingFileName));
			BufferedWriter learningDataWriter = new BufferedWriter(
					new FileWriter(this.directory.getAbsolutePath() + "/"
							+ learningFileName));
			BufferedWriter testingDataWriter = new BufferedWriter(
					new FileWriter(this.directory.getAbsolutePath() + "/"
							+ testingFileName));
			int rand;
			String line;
			while ((line = reader.readLine()) != null) {
				rand = (int) (Math.random() * 100);
				if (rand >= Config.get().sampleRate) {
					// keep data
					rand = (int) (Math.random() * 100);
					if (rand >= Config.get().splitDataRatio) {
						// store data in testing or learning file
						rand = (int) (Math.random() * 100);
						if (rand >= Config.get().splitTestRatio) {
							if (Config.get().addSentenceTags) {
								// TODO make this flexible
								line = "<s> " + line + " </s>";
							}
							learningDataWriter.write(line + "\n");
						} else {
							if (Config.get().addSentenceTags) {
								// TODO make this flexible
								line = "<s> " + line + " </s>";
							}
							testingDataWriter.write(line + "\n");
						}
					} else {
						// store data in training file
						trainingDataWriter.write(line + "\n");
					}
				}
			}
			reader.close();
			trainingDataWriter.close();
			learningDataWriter.close();
			testingDataWriter.close();

			this.logger.info("splitting done");

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void splitIntoSequences(File inputFile, int maxSequenceLength,
			int numberOfSequences) {
		System.out.println(maxSequenceLength);
		String[] fileNameSplit = inputFile.getName().split("\\.");

		HashMap<Integer, BufferedWriter> testSequenceFileWriters = new HashMap<Integer, BufferedWriter>();
		for (int i = 1; i <= maxSequenceLength; i++) {
			try {
				testSequenceFileWriters.put(i,
						new BufferedWriter(new FileWriter(new File(
								this.directory.getAbsolutePath() + "/"
										+ fileNameSplit[0] + "-samples-" + i
										+ "." + fileNameSplit[1]))));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// get total count from stats file
		long sequenceCount = 0L;
		try {
			BufferedReader reader = new BufferedReader(
					new FileReader(inputFile));
			String line;
			// count sequences
			while ((line = reader.readLine()) != null) {
				String[] lineSplit = line.split("\\s");
				if (lineSplit.length < maxSequenceLength) {
					continue;
				} else {
					int sequenceStart = 0;
					while (lineSplit.length - sequenceStart >= maxSequenceLength) {
						sequenceCount++;
						sequenceStart++;
					}
				}
			}
			reader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.logger.debug("sequenceCount: " + sequenceCount);
		double sequenceProbability = (double) numberOfSequences / sequenceCount;
		long skipDistance = sequenceCount / numberOfSequences;
		this.logger.debug("skipDistance: " + skipDistance);

		try {
			BufferedReader reader = new BufferedReader(
					new FileReader(inputFile));
			this.logger.info("splitting " + inputFile.getName()
					+ " into sequences");
			String line;
			while ((line = reader.readLine()) != null) {
				String[] originalLineSplit = line.split("\\s");
				int linePointer = 0;
				while (originalLineSplit.length - linePointer >= maxSequenceLength) {
					// build current Sequence
					String currentSequence = "";
					for (int i = 0; i < maxSequenceLength; i++) {
						currentSequence += originalLineSplit[linePointer + i]
								+ " ";
					}
					currentSequence = currentSequence.replaceFirst(" $", "");
					if (Math.random() <= sequenceProbability) {
						String[] currentSequenceSplit = currentSequence
								.split("\\s");
						for (int i = 1; i <= maxSequenceLength; i++) {
							// build result sequence
							String resultSequence = "";
							for (int j = 0; j < i; j++) {
								resultSequence += currentSequenceSplit[j] + " ";
							}
							resultSequence = resultSequence.replaceFirst(" $",
									"");
							testSequenceFileWriters.get(i).write(
									resultSequence + "\n");
						}
					}
					linePointer++;
				}
			}

			reader.close();
			for (Entry<Integer, BufferedWriter> testSequenceWritersEntry : testSequenceFileWriters
					.entrySet()) {
				testSequenceWritersEntry.getValue().close();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
