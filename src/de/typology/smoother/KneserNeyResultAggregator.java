package de.typology.smoother;

import java.io.File;
import java.util.ArrayList;

import de.typology.indexes.WordIndex;
import de.typology.utils.DecimalFormatter;

public class KneserNeyResultAggregator implements Runnable {
	private boolean[] currentPattern;
	private ArrayList<boolean[]> backoffPatterns;
	private File lowOrderResultsDirectory;
	private File tempResultsDirectory;
	private File resultDirectory;

	private WordIndex wordIndex;
	private String delimiter;
	private DecimalFormatter decimalFormatter;
	private boolean deleteTempFiles;

	public KneserNeyResultAggregator(boolean[] currentPattern,
			ArrayList<boolean[]> backoffPatterns,
			File lowOrderResultsDirectory, File tempResultsDirectory,
			File resultDirectory, WordIndex wordIndex, String delimiter,
			int decimalPlaces, boolean deleteTempFiles) {
	}

	@Override
	public void run() {

	}

}
