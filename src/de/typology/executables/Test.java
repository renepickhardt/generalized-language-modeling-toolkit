package de.typology.executables;

import java.io.IOException;

import de.typology.predictors.TypolgyMySQLSearcher;
import de.typology.utils.Config;
import de.typology.utils.IOHelper;
import de.typology.weights.WeightLearner;

public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		testTypoComplete(args);
	}

	private static void testTypoComplete(String[] args) {
		TypolgyMySQLSearcher tmss = new TypolgyMySQLSearcher();
		Config.get().weight = "no";
		for (int i = 5; i > 1; i--) {
			IOHelper.strongLog("google ngrams tested on wiki typology model parameter: "
					+ i);
			tmss.run(i, 100000, Config.get().weight);
		}
		try {
			WeightLearner.main(args);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Config.get().weight = "pic";
		for (int i = 5; i > 1; i--) {
			IOHelper.strongLog("google ngrams tested on wiki typology model parameter: "
					+ i);
			tmss.run(i, 100000, Config.get().weight);
		}
		Config.get().weight = "HMM";
		for (int i = 5; i > 1; i--) {
			IOHelper.strongLog("google ngrams tested on wiki typology model parameter: "
					+ i);
			tmss.run(i, 100000, Config.get().weight);
		}
	}
}
