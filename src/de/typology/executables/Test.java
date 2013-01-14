package de.typology.executables;

import de.typology.predictors.LMMySQLSearcher;
import de.typology.predictors.TypolgyMySQLSearcher;

public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		TypolgyMySQLSearcher.main(args);
		LMMySQLSearcher.main(args);
	}
}
