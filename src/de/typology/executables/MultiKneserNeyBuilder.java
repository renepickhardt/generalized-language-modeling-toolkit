package de.typology.executables;

import de.typology.utils.Config;

public class MultiKneserNeyBuilder {

	public static void main(String[] args) {
		String[] languages = Config.get().languages.split(",");
		String inputDataSet = Config.get().inputDataSet;
		for (String language : languages) {
			Config.get().inputDataSet = inputDataSet + "/" + language;
			KneserNeyBuilder.main(args);
		}

	}
}
