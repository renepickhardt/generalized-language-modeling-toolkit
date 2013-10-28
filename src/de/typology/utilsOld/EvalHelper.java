package de.typology.utilsOld;

public class EvalHelper {

	public static String generateFileName(String smoothingType, int n, int k,
			int subResultSize, int numberOfQueries) {
		String trainedOnDataSet = Config.get().trainedOnDataSet;
		String trainedOnLang = Config.get().trainedOnLang;
		String testedOnDataSet = Config.get().testedOnDataSet;
		String testedOnLang = Config.get().testedOnLang;

		int Sample = Config.get().sampleRate;
		int Split = Config.get().splitDataRatio;

		String fileName = "";
		fileName += "trainedOn-" + trainedOnDataSet;
		fileName += "-" + trainedOnLang;
		fileName += "-testedOn-" + testedOnDataSet;
		fileName += "-" + testedOnLang;
		fileName += "-" + smoothingType;
		fileName += "-n" + n;
		fileName += "-k" + k;
		fileName += "-sam" + Sample;
		fileName += "-split" + Split;
		fileName += "-subResultSize" + subResultSize;
		fileName += "-nQ" + numberOfQueries;
		fileName += ".log";
		return fileName;
	}
}
