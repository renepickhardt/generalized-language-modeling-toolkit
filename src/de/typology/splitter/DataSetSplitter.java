package de.typology.splitter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

import de.typology.utils.Config;
import de.typology.utils.IOHelper;

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
 * @author rpickhardt, mkoerner
 * 
 */
public class DataSetSplitter {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String outputDirectory = Config.get().outputDirectory
				+ Config.get().inputDataSet;
		DataSetSplitter dss = new DataSetSplitter(outputDirectory,
				"normalized.txt");
		dss.split(Config.get().trainingName, "learning.txt", "testing.txt", 5);

	}

	private String directory;
	private String inputName;
	protected File outputDirectory;

	public DataSetSplitter(String directory, String inputName) {

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
		IOHelper.strongLog("splitting into training, testing and learning file: "
				+ this.directory + this.inputName);
		BufferedReader reader = IOHelper.openReadFile(this.directory
				+ this.inputName);
		BufferedWriter trainingDataWriter = IOHelper.openWriteFile(
				this.directory + trainingFileName,
				Config.get().memoryLimitForWritingFiles);
		BufferedWriter learningDataWriter = IOHelper.openWriteFile(
				this.directory + learningFileName,
				Config.get().memoryLimitForWritingFiles);
		BufferedWriter testingDataWriter = IOHelper.openWriteFile(
				this.directory + testingFileName,
				Config.get().memoryLimitForWritingFiles);
		int rand;
		String line;
		try {
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
								line = "<s> " + line + " </s>";
							}
							learningDataWriter.write(line + "\n");
						} else {
							if (Config.get().addSentenceTags) {
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
			trainingDataWriter.close();
			learningDataWriter.close();
			testingDataWriter.close();
			IOHelper.strongLog("splitting done");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void splitIntoSequences(String fileName, int sequenceLength) {

		Splitter splitter;
		splitter = new Splitter(this.directory);

		String[] fileNameSplit = fileName.split("\\.");
		String newFileName = fileNameSplit[0] + "-splitted." + fileNameSplit[1];
		// get total count from stats file

		BufferedWriter writer = IOHelper.openWriteFile(this.directory
				+ newFileName, Config.get().memoryLimitForWritingFiles);
		IOHelper.strongLog("splitting " + fileName + " into sequences");
		splitter.initializeForSequenceSplit(fileName);
		long sequenceCount = 0L;
		while (splitter.getNextSequence(sequenceLength)) {
			sequenceCount++;
		}

		long skipDistance = sequenceCount / Config.get().numberOfQueries;
		splitter.initializeForSequenceSplit(fileName);
		int sequence = 0;
		int query = 0;
		while (splitter.getNextSequence(sequenceLength)) {
			if (sequence % skipDistance == 0) {
				query++;
				try {
					for (String word : splitter.sequence) {
						writer.write(word + " ");
					}
					writer.write("\n");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			sequence++;
			if (query >= Config.get().numberOfQueries) {
				break;
			}
		}
		try {
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
