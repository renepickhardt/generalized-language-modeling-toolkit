package de.typology.utils;

import java.util.Date;

public class EvalHelper {

	public static boolean badLine(String[] words, int n) {
		if (words.length < n) {
			return true;
		}
		for (int l = 0; l < n; l++) {
			words[l] = words[l].replaceAll("\\-", "\\\\-");
			words[l] = words[l].replaceAll("\\_", "\\\\_");
			words[l] = words[l].replaceAll("\\?", "\\\\?");
			if (words[l].length() < 1) {
				return true;
			}
			// if (words[l].contains("?")) {
			// System.err.println("Query word contains ?: " + words[l]);
			// return true;
			// }
		}
		return false;
	}

	public static String gennerateFileName(String type, String weigted,
			int ModelParameter, int joinLength, int numberOfQueries) {
		String trainedOnDataSet = "wiki";
		String trainedOnLang = "de";
		String testedOnDataSet = "wiki";
		String testedOnLang = "de";

		int Sample = Config.get().sampleRate;
		int Split = Config.get().splitDataRatio;

		String fileName = "";
		fileName = fileName + "trainedOn-" + trainedOnDataSet;
		fileName = fileName + "-" + trainedOnLang;
		fileName = fileName + "-testedOn-" + testedOnDataSet;
		fileName = fileName + "-" + testedOnLang;
		fileName = fileName + "-" + type;
		fileName = fileName + "-" + weigted;
		fileName = fileName + "-modelParameter" + ModelParameter;
		fileName = fileName + "-sam" + Sample;
		fileName = fileName + "-split" + Split;
		fileName = fileName + "-joinlength" + joinLength;
		fileName = fileName + "-nQ" + numberOfQueries;
		fileName = fileName + ".log";
		return fileName;
	}

	public static void openAndSetResultLogFile(String fileName) {
		// TODO: pull this data from elsewhere!
		IOHelper.setResultFile(fileName);
		IOHelper.logResult(new Date(System.currentTimeMillis()));
	}
}
