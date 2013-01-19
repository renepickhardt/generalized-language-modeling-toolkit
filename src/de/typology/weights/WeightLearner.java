package de.typology.weights;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

import de.typology.utils.IOHelper;

public class WeightLearner {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		File baseDir = new File(
				"/home/rpickhardt/data/source code/git/typology/rawlog/");
		for (String log : baseDir.list()) {
			if (log.startsWith("learnHMM-")) {
				BufferedWriter bw = IOHelper.openWriteFile(baseDir
						.getAbsolutePath()
						+ "/"
						+ log.replace("learnHMM", "HMMWeights"));
				File fin = new File(baseDir, log);
				System.out.println("Learning Weights for: ");
				System.out.println("\t" + log);
				for (int prefix = 0; prefix < 10; prefix++) {
					System.out.print(prefix + "\t0.0");
					bw.write(prefix + "\t0.0");
					LogReader reader = new LogReader(fin);
					ForwardBackward fb = new ForwardBackward(reader, prefix);
					double[] marginal = fb.computeMarginalDistribution();
					for (double element : marginal) {
						System.out.print("\t" + element);
						bw.write("\t" + element);
					}
					System.out.println();
					bw.write("\n");
				}
				bw.close();
			}
		}
	}
}
