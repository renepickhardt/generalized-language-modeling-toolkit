package de.typology.splitter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
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
		DataSetSplitter dss = new DataSetSplitter();
		String outputDirectory = Config.get().outputDirectory
				+ Config.get().inputDataSet;
		dss.split(outputDirectory + "normalized.txt", outputDirectory
				+ "training.txt", outputDirectory + "learning.txt",
				outputDirectory + "testing.txt");

	}

	private BufferedWriter trainingDataWriter;
	private BufferedWriter learningDataWriter;
	private BufferedWriter testDataWriter;

	public DataSetSplitter() {

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
	 * @param trainingFile
	 *            filename with path where training data are to be stored
	 * @param learningFile
	 *            filename with path where learning data are to be stored
	 * @param testFile
	 *            filename with path where test data are to be stored
	 */
	public void split(String inputFile, String trainingFile,
			String learningFile, String testFile) {
		IOHelper.strongLog("splitting into training, testing and learning file: "
				+ inputFile);
		BufferedReader reader = IOHelper.openReadFile(inputFile);
		this.trainingDataWriter = IOHelper.openWriteFile(trainingFile,
				Config.get().memoryLimitForWritingFiles);
		this.learningDataWriter = IOHelper.openWriteFile(learningFile,
				Config.get().memoryLimitForWritingFiles);
		this.testDataWriter = IOHelper.openWriteFile(testFile,
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
							this.learningDataWriter.write(line + "\n");
						} else {
							this.testDataWriter.write(line + "\n");
						}
					} else {
						// store data in training file
						this.trainingDataWriter.write(line + "\n");
					}
				}
			}
			this.trainingDataWriter.close();
			this.learningDataWriter.close();
			this.testDataWriter.close();
			IOHelper.strongLog("splitting done");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
