package de.typology.evaluators;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.typology.utils.Counter;

public class EntropyEvaluator {
	Logger logger = LogManager.getLogger(this.getClass().getName());

	public EntropyEvaluator() {
	}

	public void caculateEntropy(File testingSampleFile, File resultSampleFile,
			File entropyResultFile, String delimiter) {
		this.logger.info("calculate entropy for "
				+ resultSampleFile.getAbsolutePath() + " based on "
				+ testingSampleFile.getAbsolutePath());

		// count number of sequences in testingSampleFile
		long totalTestSequenceCount = Counter.countLines(testingSampleFile);
		this.logger.info("total count of test sequences: "
				+ totalTestSequenceCount);
		try {
			// count occurrences of testing sequences with format: sequence\n
			HashMap<String, Long> testSequenceMap = new HashMap<String, Long>();
			BufferedReader testSampleReader = new BufferedReader(
					new FileReader(testingSampleFile));
			String testSampleLine;
			while ((testSampleLine = testSampleReader.readLine()) != null) {
				if (testSequenceMap.containsKey(testSampleLine)) {
					testSequenceMap.put(testSampleLine,
							testSequenceMap.get(testSampleLine) + 1L);
				} else {
					testSequenceMap.put(testSampleLine, 1L);
				}
			}
			testSampleReader.close();

			BufferedReader resultSampleReader = new BufferedReader(
					new FileReader(resultSampleFile));
			String resultSampleLine;

			double entropyResult = 0.0;
			// iterate over result file with format: sequence\tprobability\n
			while ((resultSampleLine = resultSampleReader.readLine()) != null) {
				String[] resultSampleLineSplit = resultSampleLine
						.split(delimiter);
				if (!testSequenceMap.containsKey(resultSampleLineSplit[0])) {
					this.logger.error("test sequence not found: "
							+ resultSampleLineSplit[0]);
					resultSampleReader.close();
					return;
				} else {
					entropyResult += testSequenceMap
							.get(resultSampleLineSplit[0])
							/ totalTestSequenceCount
							* Math.log(Double
									.parseDouble(resultSampleLineSplit[1])
									/ Math.log(2));
				}
			}
			resultSampleReader.close();

			// negative result
			entropyResult = -entropyResult;

			BufferedWriter entropyResultWriter = new BufferedWriter(
					new FileWriter(entropyResultFile, true));
			Date date = new Date();
			String resultString = "(" + date.toString() + ") entropy of "
					+ resultSampleFile.getAbsolutePath() + " based on "
					+ testingSampleFile.getAbsolutePath() + " : "
					+ String.valueOf(entropyResult);
			this.logger.info(resultString);
			entropyResultWriter.write(resultString + "\n");
			entropyResultWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
