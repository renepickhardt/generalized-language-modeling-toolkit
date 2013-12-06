package de.typology.tester;

import java.io.File;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.typology.utils.DecimalFormatter;

public class KneserNeyTester {
	private double d1plus;

	Logger logger = LogManager.getLogger(this.getClass().getName());

	private File absoluteDirectory;
	private File _absoluteDirectory;
	private File _absolute_Directory;
	private File absolute_Directory;
	private File kneserNeyDirectory;
	private String delimiter;
	private int decimalPlaces;
	private DecimalFormatter decimalFormatter;

	// e.g. <1101,<this is test, 123>>
	private HashMap<String, HashMap<String, Long>> absoluteModelLengthFilesHashMap;

	private void loadCounts(int maxModelLength) {

	}
}
