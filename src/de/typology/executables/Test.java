package de.typology.executables;

import java.io.IOException;

import de.typology.predictors.LMMySQLSearcher;
import de.typology.predictors.TypolgyMySQLSearcher;
import de.typology.utils.Config;
import de.typology.utils.IOHelper;
import de.typology.weights.WeightLearner;

public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		frTestWikiOnGoogleComplete(args);
	}

	private static void dgttmComplete(String[] args) {
		String lang = args[0];
		Config.get().learningPath = "/home/rpickhardt/out/dgttm/" + lang
				+ "/learningSam0Split95Test50/learning.file";
		Config.get().testingPath = "/home/rpickhardt/out/dgttm/" + lang
				+ "/testSam0Split95Test50/test.file";

		Config.get().dbName = lang + "dgttmtypo";
		TypolgyMySQLSearcher tmss = new TypolgyMySQLSearcher();
		Config.get().weight = "no";
		Config.get().useWeights = false;
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
		Config.get().useWeights = true;
		for (int i = 5; i > 1; i--) {
			IOHelper.strongLog("google ngrams tested on wiki typology model parameter: "
					+ i);
			tmss.run(i, 100000, Config.get().weight);
		}
		Config.get().weight = "HMM";
		Config.get().useWeights = true;
		for (int i = 5; i > 1; i--) {
			IOHelper.strongLog("google ngrams tested on wiki typology model parameter: "
					+ i);
			tmss.run(i, 100000, Config.get().weight);
		}

		Config.get().dbName = lang + "dgttm";
		LMMySQLSearcher lmmss = new LMMySQLSearcher();

		Config.get().weight = "no";
		Config.get().useWeights = false;
		for (int i = 5; i > 1; i--) {
			IOHelper.strongLog("google ngrams tested on wiki typology model parameter: "
					+ i);
			lmmss.run(i, 100000, Config.get().weight);
		}
		try {
			WeightLearner.main(args);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Config.get().weight = "pic";
		Config.get().useWeights = true;
		for (int i = 5; i > 1; i--) {
			IOHelper.strongLog("google ngrams tested on wiki typology model parameter: "
					+ i);
			lmmss.run(i, 100000, Config.get().weight);
		}
		Config.get().weight = "HMM";
		Config.get().useWeights = true;
		for (int i = 5; i > 1; i--) {
			IOHelper.strongLog("google ngrams tested on wiki typology model parameter: "
					+ i);
			lmmss.run(i, 100000, Config.get().weight);
		}

	}

	private static void frTestWikiOnGoogleComplete(String[] args) {
		Config.get().dbName = "frgoogletypo";
		TypolgyMySQLSearcher tmss = new TypolgyMySQLSearcher();
		Config.get().weight = "no";
		Config.get().useWeights = false;
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
		Config.get().useWeights = true;
		for (int i = 5; i > 1; i--) {
			IOHelper.strongLog("google ngrams tested on wiki typology model parameter: "
					+ i);
			tmss.run(i, 100000, Config.get().weight);
		}
		Config.get().weight = "HMM";
		Config.get().useWeights = true;
		for (int i = 5; i > 1; i--) {
			IOHelper.strongLog("google ngrams tested on wiki typology model parameter: "
					+ i);
			tmss.run(i, 100000, Config.get().weight);
		}

		Config.get().dbName = "frgoogle";
		LMMySQLSearcher lmmss = new LMMySQLSearcher();

		Config.get().weight = "no";
		Config.get().useWeights = false;
		for (int i = 5; i > 1; i--) {
			IOHelper.strongLog("google ngrams tested on wiki typology model parameter: "
					+ i);
			lmmss.run(i, 100000, Config.get().weight);
		}
		try {
			WeightLearner.main(args);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Config.get().weight = "pic";
		Config.get().useWeights = true;
		for (int i = 5; i > 1; i--) {
			IOHelper.strongLog("google ngrams tested on wiki typology model parameter: "
					+ i);
			lmmss.run(i, 100000, Config.get().weight);
		}
		Config.get().weight = "HMM";
		Config.get().useWeights = true;
		for (int i = 5; i > 1; i--) {
			IOHelper.strongLog("google ngrams tested on wiki typology model parameter: "
					+ i);
			lmmss.run(i, 100000, Config.get().weight);
		}
	}

	private static void testTypoOnlyWeights(String[] args) {
		TypolgyMySQLSearcher tmss = new TypolgyMySQLSearcher();
		Config.get().weight = "pic";
		Config.get().useWeights = true;
		for (int i = 5; i > 1; i--) {
			IOHelper.strongLog("google ngrams tested on wiki typology model parameter: "
					+ i);
			tmss.run(i, 100000, Config.get().weight);
		}
		Config.get().weight = "HMM";
		Config.get().useWeights = true;
		for (int i = 5; i > 1; i--) {
			IOHelper.strongLog("google ngrams tested on wiki typology model parameter: "
					+ i);
			tmss.run(i, 100000, Config.get().weight);
		}
	}

	private static void testTypoComplete(String[] args) {
		TypolgyMySQLSearcher tmss = new TypolgyMySQLSearcher();
		Config.get().weight = "no";
		Config.get().useWeights = false;
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
	}
}
