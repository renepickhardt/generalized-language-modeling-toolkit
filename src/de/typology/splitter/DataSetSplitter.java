package de.typology.splitter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

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

	static Logger logger = LogManager.getLogger(Aggregator.class.getName());

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
		logger.info("splitting into training, testing and learning file: "
				+ this.directory + this.inputName);
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
			logger.info("splitting done");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void splitIntoSequences(File inputFile, int sequenceLength,
			int numberOfSequences) {

		String[] fileNameSplit = inputFile.getName().split("\\.");
		String newFileName = fileNameSplit[0] + "-splitted." + fileNameSplit[1];
		// get total count from stats file

		long sequenceCount = 0L;
		try {
			BufferedReader reader = new BufferedReader(
					new FileReader(inputFile));
			String line;
			// count sequences
			while ((line = reader.readLine()) != null) {
				String[] lineSplit = line.split("\\s");
				if (lineSplit.length < sequenceLength) {
					continue;
				} else {
					int sequenceStart = 0;
					while (lineSplit.length - sequenceStart >= sequenceLength) {
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
		logger.debug("sequenceCount: " + sequenceCount);
		long skipDistance = sequenceCount / numberOfSequences;
		logger.debug("skipDistance: " + skipDistance);

		// BufferedWriter writer = new BufferedWriter(new FileWriter(
		// this.directory.getAbsolutePath() + "/" + newFileName));
		// logger.info("splitting " + inputFile.getName() + " into sequences");
		// int sequence = 0;
		// int query = 0;
		// while (splitter.getNextSequence(sequenceLength)) {
		// if (sequence % skipDistance == 0) {
		// query++;
		// try {
		// for (String word : splitter.sequence) {
		// writer.write(word + " ");
		// }
		// writer.write("\n");
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
		// }
		// sequence++;
		// if (query >= Config.get().numberOfQueries) {
		// break;
		// }
		// }
		// try {
		// writer.close();
		// } catch (IOException e) {
		// e.printStackTrace();
		// }

	}
}