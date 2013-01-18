package de.typology.weights;

import java.io.File;
import java.io.IOException;

public class WeightLearner {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		File baseDir = new File("/home/gottron/Data/typology/");
		String[] logs = {
				"learnHMM-trainedOn-wiki-en-testedOn-wiki-en-lm-no-modelParameter5-sam0-split95-joinlength10-nQ100000.log" 
//				, "learnHMM-trainedOn-wiki-en-testedOn-wiki-en-lm-pic-modelParameter5-sam0-split95-joinlength10-nQ100000.log"
		};
		for (String log : logs) {
			File fin = new File (baseDir, log);
			System.out.println("Learning Weights for: ");
			System.out.println("\t"+log);
			for (int prefix = 0; prefix < 10; prefix++) {
				System.out.println("\tPrefix "+prefix);
				LogReader reader = new LogReader(fin);
				ForwardBackward fb = new ForwardBackward(reader,prefix);
				double[] marginal = fb.computeMarginalDistribution();
				System.out.println("Weights: ");
				for (int i = 0; i< marginal.length; i++) {
					System.out.println((i+1)+" -- "+ marginal[i]);
				}
				System.out.println();
			}
		}
	}

	
}
