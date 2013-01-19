package de.typology.weights;

import java.io.File;
import java.io.IOException;

public class LogTester {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		File baseDir = new File("/home/gottron/Data/typology/");
		File fin = new File (baseDir, "learnHMM-trainedOn-wiki-en-testedOn-wiki-en-lm-no-modelParameter5-sam0-split95-joinlength10-nQ100000.log");
		LogReader reader = new LogReader(fin);
		double[][] entry = null;
		int cnt = 0;
		while ( (entry = reader.getNextProbabilities()) != null) {
			System.out.println(cnt+"\t"+entry[0][0]);
			cnt++;
		}
		
	}

}
